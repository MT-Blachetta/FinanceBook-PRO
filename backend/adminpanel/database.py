"""
Database configuration for admin panel.

The admin panel connects to the same PostgreSQL database as the Java backend.
It uses SQLModel (which is built on SQLAlchemy) for database operations.
"""

import os
from typing import Generator

from sqlmodel import Session, create_engine

# Database connection settings
DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://yourself:secretPassword@localhost:5500/financebook"
)

# Create database engine
engine = create_engine(DATABASE_URL, echo=False)


def get_session() -> Generator[Session, None, None]:
    """Dependency to get database session."""
    with Session(engine) as session:
        yield session
