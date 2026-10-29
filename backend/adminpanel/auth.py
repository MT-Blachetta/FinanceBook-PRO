"""
Authentication helpers for admin panel.

Provides password hashing/verification using bcrypt and
session management using itsdangerous.

Hashes are standard bcrypt strings and are interchangeable with the Java
backend's BCryptPasswordEncoder: users created here can log in to the app and
vice versa.
"""

import os
from typing import Optional

import bcrypt

# Secret key for session signing
SECRET_KEY = os.getenv("ADMIN_SESSION_SECRET", "change-this-secret-for-admin-sessions")

# Password policy - must match com.financebook.util.PasswordPolicy in the Java backend.
MIN_PASSWORD_LENGTH = 6
MAX_PASSWORD_BYTES = 72  # bcrypt only uses the first 72 bytes of a password


def password_policy_error(password: str) -> Optional[str]:
    """Return an error message if *password* violates the policy, else None."""
    if len(password) < MIN_PASSWORD_LENGTH:
        return f"Password must be at least {MIN_PASSWORD_LENGTH} characters"
    if len(password.encode("utf-8")) > MAX_PASSWORD_BYTES:
        return f"Password must not be longer than {MAX_PASSWORD_BYTES} bytes"
    return None


def hash_password(password: str) -> str:
    """Hash a password using bcrypt."""
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(password.encode('utf-8'), salt)
    return hashed.decode('utf-8')


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a password against a bcrypt hash."""
    try:
        return bcrypt.checkpw(
            plain_password.encode('utf-8'),
            hashed_password.encode('utf-8')
        )
    except Exception:
        return False
