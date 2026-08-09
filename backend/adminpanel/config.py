"""
Configuration for admin panel.

Defines the connection to the Java backend API.
"""

import os

# Java Backend API Configuration
JAVA_BACKEND_URL = os.getenv("JAVA_BACKEND_URL", "http://localhost:8000")

# Database Configuration
DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://yourself:secretPassword@localhost:5500/financebook"
)

# Session Configuration
SESSION_SECRET = os.getenv("ADMIN_SESSION_SECRET", "change-this-secret-for-admin-sessions")
SESSION_MAX_AGE = int(os.getenv("ADMIN_SESSION_MAX_AGE", "3600"))  # 1 hour

# Admin Panel Configuration
ADMIN_HOST = os.getenv("ADMIN_HOST", "0.0.0.0")
ADMIN_PORT = int(os.getenv("ADMIN_PORT", "8080"))
