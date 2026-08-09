"""
Authentication helpers for admin panel.

Provides password hashing/verification using bcrypt and
session management using itsdangerous.
"""

import os
import bcrypt

# Secret key for session signing
SECRET_KEY = os.getenv("ADMIN_SESSION_SECRET", "change-this-secret-for-admin-sessions")


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
