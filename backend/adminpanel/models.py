"""
Database models for admin panel.

These models match the same database schema used by the Java backend.
Table names match Java JPA naming (lowercase, no underscores).
"""

from datetime import datetime
from typing import Optional

from sqlmodel import Field, SQLModel


class User(SQLModel, table=True):
    """User model matching Java backend User entity."""
    __tablename__ = "user"
    
    id: Optional[int] = Field(default=None, primary_key=True)
    username: str = Field(index=True, unique=True, max_length=100)
    hashed_password: str
    surname: str = Field(max_length=100)
    prename: str = Field(max_length=100)
    city: Optional[str] = Field(default=None, max_length=100)
    phone: Optional[str] = Field(default=None, max_length=50)
    is_admin: bool = Field(default=False)
    is_active: bool = Field(default=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)


class PaymentItem(SQLModel, table=True):
    """Payment item model for statistics."""
    __tablename__ = "paymentitem"  # Java uses lowercase, no underscore
    
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id")
    amount: float
    date: datetime
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
    name: str = Field(max_length=100)
    type_id: int = Field(foreign_key="categorytype.id")
    parent_id: Optional[int] = Field(default=None, foreign_key="category.id")
    icon_file: Optional[str] = Field(default=None, max_length=255)


class CategoryType(SQLModel, table=True):
    """Category type model."""
    __tablename__ = "categorytype"  # Java uses lowercase, no underscore
    
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id")
    name: str = Field(max_length=100)
    description: Optional[str] = None


class TransactionFeePlan(SQLModel, table=True):
    """Transaction fee plan model."""
    __tablename__ = "transactionfeeplan"  # Java uses lowercase, no underscore
    
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id")
    mode: str = Field(default="table", max_length=20)
    formula_text: Optional[str] = None
    amount_table_json: str = Field(default="[0]")
    interval_data_json: str = Field(default="{}")
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)


class TransactionFeeRecord(SQLModel, table=True):
    """Transaction fee record model."""
    __tablename__ = "transactionfeerecord"  # Java uses lowercase, no underscore
    
    id: Optional[int] = Field(default=None, primary_key=True)
    payment_item_id: int = Field(foreign_key="paymentitem.id")
    user_id: int = Field(foreign_key="user.id")
    fee_amount: float
    original_amount: float
    created_at: datetime = Field(default_factory=datetime.utcnow)
