"""
Database models for admin panel.

These models match the same database schema used by the Java backend
(backend/src/main/resources/db/migration). Table names match Java JPA naming
(lowercase, no underscores).

Timestamps
----------
All timestamp columns are ``TIMESTAMP WITHOUT TIME ZONE`` holding the server's
local time, exactly like the Java backend writes them (``LocalDateTime.now()``).
They are therefore mapped explicitly to a naive ``DateTime`` column type and
filled with :func:`local_now`. (Recent SQLModel versions otherwise treat plain
``datetime`` fields as timezone-aware UTC and reject naive values.)
"""

from datetime import date, datetime
from decimal import Decimal
from typing import Any, Optional

from sqlalchemy import DateTime
from sqlmodel import Field, SQLModel

# Field length limits - must match the database schema and
# com.financebook.util.Constants in the Java backend.
MAX_USERNAME_LENGTH = 50
MAX_USER_NAME_LENGTH = 100
MAX_USER_PHONE_LENGTH = 30
MAX_USER_ROAD_LENGTH = 200
MAX_USER_HOUSE_NUMBER_LENGTH = 20
MAX_USER_REGION_LENGTH = 100
MAX_USER_POSTAL_LENGTH = 20
MAX_USER_CITY_LENGTH = 100
MAX_USER_STATE_LENGTH = 100


def local_now() -> datetime:
    """Current server-local time without tzinfo (matches Java's LocalDateTime.now())."""
    return datetime.now()


def _timestamp_field() -> Any:
    return Field(default_factory=local_now, sa_type=DateTime(timezone=False))


class User(SQLModel, table=True):
    """User model matching Java backend User entity."""
    __tablename__ = "user"

    id: Optional[int] = Field(default=None, primary_key=True)
    username: str = Field(index=True, unique=True, max_length=MAX_USERNAME_LENGTH)
    hashed_password: str = Field(max_length=255)
    surname: str = Field(max_length=MAX_USER_NAME_LENGTH)
    prename: str = Field(max_length=MAX_USER_NAME_LENGTH)
    birth_date: Optional[date] = None
    phone: Optional[str] = Field(default=None, max_length=MAX_USER_PHONE_LENGTH)
    road: Optional[str] = Field(default=None, max_length=MAX_USER_ROAD_LENGTH)
    house_number: Optional[str] = Field(default=None, max_length=MAX_USER_HOUSE_NUMBER_LENGTH)
    region: Optional[str] = Field(default=None, max_length=MAX_USER_REGION_LENGTH)
    postal: Optional[str] = Field(default=None, max_length=MAX_USER_POSTAL_LENGTH)
    city: Optional[str] = Field(default=None, max_length=MAX_USER_CITY_LENGTH)
    state: Optional[str] = Field(default=None, max_length=MAX_USER_STATE_LENGTH)
    is_admin: bool = Field(default=False)
    is_active: bool = Field(default=True)
    created_at: datetime = _timestamp_field()


class PaymentItem(SQLModel, table=True):
    """Payment item model for statistics."""
    __tablename__ = "paymentitem"  # Java uses lowercase, no underscore

    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id")
    amount: Decimal = Field(max_digits=10, decimal_places=2)
    date: datetime = Field(sa_type=DateTime(timezone=False))
    description: Optional[str] = None
    periodic: bool = Field(default=False)


class Recipient(SQLModel, table=True):
    """Recipient model for statistics."""
    __tablename__ = "recipient"

    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id")
    name: str = Field(max_length=255)
    address: Optional[str] = Field(default=None, max_length=500)


class Category(SQLModel, table=True):
    """Category model for statistics."""
    __tablename__ = "category"

    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id")
    name: str = Field(max_length=255)
    type_id: int = Field(foreign_key="categorytype.id")
    parent_id: Optional[int] = Field(default=None, foreign_key="category.id")
    icon_file: Optional[str] = Field(default=None, max_length=255)


class CategoryType(SQLModel, table=True):
    """Category type model."""
    __tablename__ = "categorytype"  # Java uses lowercase, no underscore

    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id")
    name: str = Field(max_length=255)
    description: Optional[str] = Field(default=None, max_length=1000)


class TransactionFeePlan(SQLModel, table=True):
    """Transaction fee plan model (one per user)."""
    __tablename__ = "transactionfeeplan"  # Java uses lowercase, no underscore

    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id", unique=True)
    mode: str = Field(default="table", max_length=20)
    formula_text: Optional[str] = None
    amount_table_json: str = Field(default="[0]")
    interval_data_json: str = Field(default="{}")
    created_at: datetime = _timestamp_field()
    updated_at: datetime = _timestamp_field()


class TransactionFeeRecord(SQLModel, table=True):
    """Transaction fee record model (at most one per payment item)."""
    __tablename__ = "transactionfeerecord"  # Java uses lowercase, no underscore

    id: Optional[int] = Field(default=None, primary_key=True)
    payment_item_id: int = Field(foreign_key="paymentitem.id", unique=True)
    user_id: int = Field(foreign_key="user.id")
    fee_amount: Decimal = Field(max_digits=10, decimal_places=2)
    original_amount: Decimal = Field(max_digits=10, decimal_places=2)
    created_at: datetime = _timestamp_field()
