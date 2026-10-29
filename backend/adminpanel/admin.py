"""
Admin website router for FinanceBook.

Serves Jinja2 HTML templates for the administration panel, secured by
session-based authentication (itsdangerous signed cookies).

The admin panel allows:
  • Viewing all registered users
  • Creating users and editing their details (name, birth date, address, phone)
  • Resetting user passwords
  • Granting / revoking admin rights
  • Activating / deactivating user accounts
  • Configuring each user's transaction fee plan
"""
from __future__ import annotations

import json
import math
from datetime import date, datetime
from pathlib import Path
from typing import Optional

from fastapi import APIRouter, Depends, Form, HTTPException, Request
from fastapi.responses import HTMLResponse, JSONResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from itsdangerous import URLSafeTimedSerializer, BadSignature, SignatureExpired
from sqlalchemy import func
from sqlalchemy.exc import IntegrityError
from sqlmodel import Session, select

from auth import hash_password, password_policy_error, verify_password, SECRET_KEY
from database import get_session
from models import (
    User, PaymentItem, Recipient, Category, CategoryType, TransactionFeePlan,
    MAX_USERNAME_LENGTH, MAX_USER_NAME_LENGTH, MAX_USER_PHONE_LENGTH,
    MAX_USER_ROAD_LENGTH, MAX_USER_HOUSE_NUMBER_LENGTH, MAX_USER_REGION_LENGTH,
    MAX_USER_POSTAL_LENGTH, MAX_USER_CITY_LENGTH, MAX_USER_STATE_LENGTH,
    local_now,
)
from fee_engine import validate_formula

# ─── Configuration ───────────────────────────────────────────────────

TEMPLATES_DIR = Path(__file__).parent / "templates"
TEMPLATES_DIR.mkdir(exist_ok=True)

templates = Jinja2Templates(directory=str(TEMPLATES_DIR))

# Session cookie signer (uses the same secret as JWT for simplicity)
_signer = URLSafeTimedSerializer(SECRET_KEY)
SESSION_COOKIE = "admin_session"
SESSION_MAX_AGE = 3600  # 1 hour

# Largest payment amount the database can hold (NUMERIC(10,2))
MAX_AMOUNT = 99_999_999.99

admin_router = APIRouter(prefix="/admin", tags=["Admin Website"])

# Optional profile fields: form name -> (label, max length)
_OPTIONAL_PROFILE_FIELDS = {
    "phone": ("Phone", MAX_USER_PHONE_LENGTH),
    "road": ("Street", MAX_USER_ROAD_LENGTH),
    "house_number": ("House number", MAX_USER_HOUSE_NUMBER_LENGTH),
    "region": ("Region", MAX_USER_REGION_LENGTH),
    "postal": ("Postal code", MAX_USER_POSTAL_LENGTH),
    "city": ("City", MAX_USER_CITY_LENGTH),
    "state": ("State / country", MAX_USER_STATE_LENGTH),
}


# ─── Helpers ─────────────────────────────────────────────────────────

def _get_admin_username(request: Request) -> Optional[str]:
    """Extract and verify the admin session cookie. Returns username or None."""
    token = request.cookies.get(SESSION_COOKIE)
    if not token:
        return None
    try:
        username = _signer.loads(token, max_age=SESSION_MAX_AGE)
        return username
    except (BadSignature, SignatureExpired):
        return None


def _require_admin(request: Request, session: Session) -> User:
    """Verify admin session or redirect to login."""
    username = _get_admin_username(request)
    if not username:
        raise HTTPException(status_code=302, headers={"Location": "/admin/login"})
    user = session.exec(select(User).where(User.username == username)).first()
    if not user or not user.is_admin or not user.is_active:
        raise HTTPException(status_code=302, headers={"Location": "/admin/login"})
    return user


def _normalize(text: str) -> str:
    """Trim and collapse internal whitespace (mirrors Java's NameNormalizer)."""
    return " ".join(text.split())


def _parse_birth_date(raw: str) -> Optional[date]:
    """Parse a birth date given as YYYY-MM-DD or DD.MM.YYYY. Empty -> None."""
    raw = raw.strip()
    if not raw:
        return None
    for fmt in ("%Y-%m-%d", "%d.%m.%Y"):
        try:
            parsed = datetime.strptime(raw, fmt).date()
            break
        except ValueError:
            continue
    else:
        raise ValueError("Invalid date of birth (use YYYY-MM-DD or DD.MM.YYYY)")
    if parsed > date.today():
        raise ValueError("Date of birth cannot be in the future")
    return parsed


def _clean_profile(form: dict) -> tuple[dict, Optional[str]]:
    """Validate and normalize the profile fields shared by create and edit.

    Returns ``(values, error)`` where *values* maps model attribute names to
    cleaned values ready to be set on a :class:`User`.
    """
    values: dict = {}
    for name, label in (("prename", "First name"), ("surname", "Last name")):
        value = _normalize(form[name])
        if not value:
            return values, "First name and last name are required"
        if len(value) > MAX_USER_NAME_LENGTH:
            return values, f"{label} must not exceed {MAX_USER_NAME_LENGTH} characters"
        values[name] = value

    for name, (label, max_len) in _OPTIONAL_PROFILE_FIELDS.items():
        value = form[name].strip()
        if len(value) > max_len:
            return values, f"{label} must not exceed {max_len} characters"
        values[name] = value or None

    try:
        values["birth_date"] = _parse_birth_date(form["birth_date"])
    except ValueError as exc:
        return values, str(exc)

    return values, None


def _count(session: Session, model, user_id: Optional[int] = None) -> int:
    """Count rows of *model*, optionally restricted to one user."""
    query = select(func.count()).select_from(model)
    if user_id is not None:
        query = query.where(model.user_id == user_id)
    return session.exec(query).one()


def _render_user_detail(request: Request, session: Session, admin: User, user: User,
                        success: Optional[str] = None, error: Optional[str] = None):
    return templates.TemplateResponse(request, "user_detail.html", {
        "admin": admin,
        "user": user,
        "payment_count": _count(session, PaymentItem, user.id),
        "recipient_count": _count(session, Recipient, user.id),
        "category_count": _count(session, Category, user.id),
        "success": success,
        "error": error,
    })


# ─── Routes ──────────────────────────────────────────────────────────

@admin_router.get("/login", response_class=HTMLResponse)
def admin_login_page(request: Request):
    """Render the admin login page."""
    # If already logged in, redirect to dashboard
    if _get_admin_username(request):
        return RedirectResponse(url="/admin/dashboard", status_code=302)
    return templates.TemplateResponse(request, "login.html", {"error": None})


@admin_router.post("/login", response_class=HTMLResponse)
def admin_login_submit(
    request: Request,
    username: str = Form(...),
    password: str = Form(...),
    session: Session = Depends(get_session),
):
    """Process the admin login form."""
    user = session.exec(select(User).where(User.username == username.strip())).first()
    if not user or not verify_password(password, user.hashed_password):
        return templates.TemplateResponse(
            request,
            "login.html",
            {"error": "Invalid username or password"},
        )
    if not user.is_admin:
        return templates.TemplateResponse(
            request,
            "login.html",
            {"error": "You do not have admin privileges"},
        )
    if not user.is_active:
        return templates.TemplateResponse(
            request,
            "login.html",
            {"error": "Your account has been deactivated"},
        )

    # Set signed session cookie
    token = _signer.dumps(user.username)
    response = RedirectResponse(url="/admin/dashboard", status_code=302)
    response.set_cookie(
        SESSION_COOKIE,
        token,
        max_age=SESSION_MAX_AGE,
        httponly=True,
        samesite="lax",
    )
    return response


@admin_router.get("/logout")
def admin_logout():
    """Clear the admin session and redirect to login."""
    response = RedirectResponse(url="/admin/login", status_code=302)
    response.delete_cookie(SESSION_COOKIE)
    return response


@admin_router.get("/dashboard", response_class=HTMLResponse)
def admin_dashboard(
    request: Request,
    session: Session = Depends(get_session),
):
    """Admin dashboard with overview statistics."""
    admin = _require_admin(request, session)

    active_users = session.exec(
        select(func.count()).select_from(User).where(User.is_active == True)  # noqa: E712
    ).one()

    return templates.TemplateResponse(request, "dashboard.html", {
        "admin": admin,
        "total_users": _count(session, User),
        "active_users": active_users,
        "total_payments": _count(session, PaymentItem),
        "total_recipients": _count(session, Recipient),
        "total_categories": _count(session, Category),
    })


@admin_router.get("/users", response_class=HTMLResponse)
def admin_users_list(
    request: Request,
    search: str = "",
    session: Session = Depends(get_session),
):
    """List all users with optional (case-insensitive) search."""
    admin = _require_admin(request, session)

    query = select(User)
    term = search.strip()
    if term:
        query = query.where(
            (User.username.icontains(term))
            | (User.surname.icontains(term))
            | (User.prename.icontains(term))
        )
    users = session.exec(query.order_by(User.id)).all()

    return templates.TemplateResponse(request, "users.html", {
        "admin": admin,
        "users": users,
        "search": search,
    })


@admin_router.get("/users/create", response_class=HTMLResponse)
def admin_user_create_page(
    request: Request,
    session: Session = Depends(get_session),
):
    """Render the create user form."""
    admin = _require_admin(request, session)
    return templates.TemplateResponse(request, "user_create.html", {
        "admin": admin,
        "form": {"is_active": True},
        "error": None,
    })


@admin_router.post("/users/create", response_class=HTMLResponse)
def admin_user_create_submit(
    request: Request,
    username: str = Form(...),
    password: str = Form(...),
    surname: str = Form(...),
    prename: str = Form(...),
    phone: str = Form(""),
    birth_date: str = Form(""),
    road: str = Form(""),
    house_number: str = Form(""),
    region: str = Form(""),
    postal: str = Form(""),
    city: str = Form(""),
    state: str = Form(""),
    # Unchecked checkboxes are not submitted at all, hence the "off" defaults
    is_admin: str = Form("off"),
    is_active: str = Form("off"),
    session: Session = Depends(get_session),
):
    """Process the create user form."""
    admin = _require_admin(request, session)

    # Collect form data for re-rendering on error
    form_data = {
        "username": username, "prename": prename, "surname": surname,
        "phone": phone, "birth_date": birth_date, "road": road,
        "house_number": house_number, "region": region, "postal": postal,
        "city": city, "state": state,
        "is_admin": is_admin == "on", "is_active": is_active == "on",
    }

    def _render_error(msg: str):
        return templates.TemplateResponse(request, "user_create.html", {
            "admin": admin, "form": form_data, "error": msg,
        })

    # Validate
    clean_username = _normalize(username)
    if not clean_username:
        return _render_error("Username cannot be empty")
    if len(clean_username) > MAX_USERNAME_LENGTH:
        return _render_error(f"Username must not exceed {MAX_USERNAME_LENGTH} characters")
    if session.exec(select(User).where(User.username == clean_username)).first():
        return _render_error("Username already taken")
    password_error = password_policy_error(password)
    if password_error:
        return _render_error(password_error)
    profile, profile_error = _clean_profile(form_data)
    if profile_error:
        return _render_error(profile_error)

    # Create the user together with its default category type and
    # UNCLASSIFIED category in one transaction (same as Java's /auth/register).
    new_user = User(
        username=clean_username,
        hashed_password=hash_password(password),
        is_admin=form_data["is_admin"],
        is_active=form_data["is_active"],
        **profile,
    )
    try:
        session.add(new_user)
        session.flush()

        standard_type = CategoryType(
            name="standard",
            description="Default category type for basic expense/income classification",
            user_id=new_user.id,
        )
        session.add(standard_type)
        session.flush()

        session.add(Category(
            name="UNCLASSIFIED",
            type_id=standard_type.id,
            parent_id=None,
            user_id=new_user.id,
        ))
        session.commit()
    except IntegrityError:
        session.rollback()
        return _render_error("Username already taken")

    return RedirectResponse(
        url=f"/admin/users/{new_user.id}?success=User+created+successfully",
        status_code=302,
    )


@admin_router.get("/users/{user_id}", response_class=HTMLResponse)
def admin_user_detail(
    user_id: int,
    request: Request,
    success: str = "",
    error: str = "",
    session: Session = Depends(get_session),
):
    """User detail / edit form."""
    admin = _require_admin(request, session)

    user = session.get(User, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    return _render_user_detail(request, session, admin, user,
                               success=success or None, error=error or None)


@admin_router.post("/users/{user_id}", response_class=HTMLResponse)
def admin_user_update(
    user_id: int,
    request: Request,
    surname: str = Form(...),
    prename: str = Form(...),
    phone: str = Form(""),
    birth_date: str = Form(""),
    road: str = Form(""),
    house_number: str = Form(""),
    region: str = Form(""),
    postal: str = Form(""),
    city: str = Form(""),
    state: str = Form(""),
    new_password: str = Form(""),
    # Unchecked checkboxes are not submitted at all, hence the "off" defaults
    is_admin: str = Form("off"),
    is_active: str = Form("off"),
    session: Session = Depends(get_session),
):
    """Save user changes from the detail form.

    All input is validated first; on any error nothing is saved.
    """
    admin = _require_admin(request, session)

    user = session.get(User, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    profile, error = _clean_profile({
        "prename": prename, "surname": surname, "phone": phone,
        "birth_date": birth_date, "road": road, "house_number": house_number,
        "region": region, "postal": postal, "city": city, "state": state,
    })
    if not error and new_password:
        error = password_policy_error(new_password)
    if not error and user.id == admin.id and is_active != "on":
        error = "You cannot deactivate your own account"
    if not error and user.id == admin.id and is_admin != "on":
        error = "You cannot remove your own admin rights"
    if error:
        return _render_user_detail(request, session, admin, user, error=error)

    for name, value in profile.items():
        setattr(user, name, value)
    user.is_admin = is_admin == "on"
    user.is_active = is_active == "on"
    if new_password:
        user.hashed_password = hash_password(new_password)

    session.add(user)
    session.commit()
    session.refresh(user)

    return _render_user_detail(request, session, admin, user,
                               success="User updated successfully")


@admin_router.post("/users/{user_id}/deactivate", response_class=HTMLResponse)
def admin_user_toggle_active(
    user_id: int,
    request: Request,
    session: Session = Depends(get_session),
):
    """Toggle user active status."""
    admin = _require_admin(request, session)

    user = session.get(User, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    if user.id == admin.id:
        return RedirectResponse(url=f"/admin/users/{user_id}?error=Cannot+deactivate+yourself", status_code=302)

    user.is_active = not user.is_active
    session.add(user)
    session.commit()

    message = "User+activated" if user.is_active else "User+deactivated"
    return RedirectResponse(url=f"/admin/users/{user_id}?success={message}", status_code=302)


# ─── Fee Configuration ──────────────────────────────────────────────

def _is_number(value) -> bool:
    return (isinstance(value, (int, float)) and not isinstance(value, bool)
            and math.isfinite(value))


def _fee_plan_error(body: dict) -> Optional[str]:
    """Validate a fee plan payload from the fee configuration page."""
    mode = body.get("mode", "table")
    if mode not in ("table", "formula"):
        return "mode must be 'table' or 'formula'"

    if mode == "formula":
        formula = body.get("formula_text")
        if not isinstance(formula, str) or not validate_formula(formula):
            return "Invalid fee formula"

    table = body.get("amount_table", [0])
    if (not isinstance(table, list) or not table
            or not all(_is_number(v) for v in table)):
        return "amount_table must be a non-empty list of numbers"
    if table[0] != 0:
        return "amount_table must start with 0"
    if any(b <= a for a, b in zip(table, table[1:])):
        return "amount_table must be strictly increasing"
    if table[-1] > MAX_AMOUNT:
        return f"amount_table values must not exceed {MAX_AMOUNT}"

    intervals = body.get("interval_data", {})
    if not isinstance(intervals, dict):
        return "interval_data must be an object"
    for key, idata in intervals.items():
        if not isinstance(idata, dict):
            return f"interval_data[{key}] must be an object"
        max_fee = idata.get("maxFee", 0.1)
        if not _is_number(max_fee) or max_fee <= 0:
            return f"interval_data[{key}].maxFee must be a positive number"
        coefficients = idata.get("coefficients")
        if coefficients is not None and (
                not isinstance(coefficients, list)
                or not all(_is_number(c) for c in coefficients)):
            return f"interval_data[{key}].coefficients must be a list of numbers"
        points = idata.get("points", [])
        if not isinstance(points, list) or not all(
                isinstance(p, dict) and _is_number(p.get("freq")) and _is_number(p.get("fee"))
                for p in points):
            return f"interval_data[{key}].points must be a list of {{freq, fee}} numbers"
    return None


def _require_user(session: Session, user_id: int) -> User:
    user = session.get(User, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user


@admin_router.get("/fees", response_class=HTMLResponse)
def admin_fees_page(
    request: Request,
    session: Session = Depends(get_session),
):
    """Render the transaction fee configuration page."""
    admin = _require_admin(request, session)
    users = session.exec(select(User).order_by(User.id)).all()

    return templates.TemplateResponse(request, "fee_config.html", {
        "admin": admin,
        "users": users,
    })


@admin_router.get("/api/fee-plan/{user_id}")
def get_fee_plan(
    user_id: int,
    request: Request,
    session: Session = Depends(get_session),
):
    """Return the fee plan for a specific user as JSON."""
    _require_admin(request, session)
    _require_user(session, user_id)

    plan = session.exec(
        select(TransactionFeePlan).where(TransactionFeePlan.user_id == user_id)
    ).first()

    if not plan:
        return {
            "mode": "table",
            "formula_text": "",
            "amount_table": [0],
            "interval_data": {},
        }

    return {
        "mode": plan.mode,
        "formula_text": plan.formula_text or "",
        "amount_table": json.loads(plan.amount_table_json),
        "interval_data": json.loads(plan.interval_data_json),
    }


@admin_router.put("/api/fee-plan/{user_id}")
def save_fee_plan(
    user_id: int,
    request: Request,
    body: dict,
    session: Session = Depends(get_session),
):
    """Save/update the fee plan for a specific user."""
    _require_admin(request, session)
    _require_user(session, user_id)

    error = _fee_plan_error(body)
    if error:
        return JSONResponse(status_code=400, content={"detail": error})

    plan = session.exec(
        select(TransactionFeePlan).where(TransactionFeePlan.user_id == user_id)
    ).first()

    if not plan:
        plan = TransactionFeePlan(user_id=user_id)

    plan.mode = body.get("mode", "table")
    plan.formula_text = body.get("formula_text") if plan.mode == "formula" else None
    plan.amount_table_json = json.dumps(body.get("amount_table", [0]))
    plan.interval_data_json = json.dumps(body.get("interval_data", {}))
    plan.updated_at = local_now()

    session.add(plan)
    session.commit()

    return {"status": "ok"}


@admin_router.post("/api/fee-plan/{user_id}/validate-formula")
def validate_formula_endpoint(
    user_id: int,
    request: Request,
    body: dict,
    session: Session = Depends(get_session),
):
    """Validate a fee formula expression."""
    _require_admin(request, session)

    formula = body.get("formula", "")
    is_valid = isinstance(formula, str) and validate_formula(formula)

    return {"valid": is_valid}
