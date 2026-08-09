-- FinanceBook Initial Schema Migration
-- Creates all tables for the multi-user finance management application

-- User table
CREATE TABLE "user" (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    surname VARCHAR(100) NOT NULL,
    prename VARCHAR(100) NOT NULL,
    birth_date DATE,
    phone VARCHAR(30),
    road VARCHAR(200),
    house_number VARCHAR(20),
    region VARCHAR(100),
    postal VARCHAR(20),
    city VARCHAR(100),
    state VARCHAR(100),
    is_admin BOOLEAN DEFAULT FALSE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Category Type table
CREATE TABLE categorytype (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    user_id BIGINT REFERENCES "user"(id) NOT NULL
);

-- Category table (hierarchical)
CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type_id BIGINT REFERENCES categorytype(id) NOT NULL,
    parent_id BIGINT REFERENCES category(id),
    icon_file VARCHAR(255),
    user_id BIGINT REFERENCES "user"(id) NOT NULL
);

-- Recipient table
CREATE TABLE recipient (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    user_id BIGINT REFERENCES "user"(id) NOT NULL
);

-- Payment Item table
CREATE TABLE paymentitem (
    id BIGSERIAL PRIMARY KEY,
    amount DECIMAL(10,2) NOT NULL,
    date TIMESTAMP NOT NULL,
    periodic BOOLEAN DEFAULT FALSE NOT NULL,
    description VARCHAR(1000),
    invoice_path VARCHAR(255),
    product_image_path VARCHAR(255),
    recipient_id BIGINT REFERENCES recipient(id),
    standard_category_id BIGINT REFERENCES category(id),
    user_id BIGINT REFERENCES "user"(id) NOT NULL
);

-- Many-to-many link table
CREATE TABLE paymentitemcategorylink (
    payment_item_id BIGINT REFERENCES paymentitem(id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES category(id),
    PRIMARY KEY (payment_item_id, category_id)
);

-- Transaction Fee Plan table
CREATE TABLE transactionfeeplan (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES "user"(id) UNIQUE NOT NULL,
    mode VARCHAR(20) DEFAULT 'table' NOT NULL,
    formula_text TEXT,
    amount_table_json TEXT DEFAULT '[0]' NOT NULL,
    interval_data_json TEXT DEFAULT '{}' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Transaction Fee Record table
CREATE TABLE transactionfeerecord (
    id BIGSERIAL PRIMARY KEY,
    payment_item_id BIGINT REFERENCES paymentitem(id) NOT NULL,
    user_id BIGINT REFERENCES "user"(id) NOT NULL,
    fee_amount DECIMAL(10,2) NOT NULL,
    original_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create indexes for better query performance
CREATE INDEX idx_user_username ON "user"(username);
CREATE INDEX idx_user_is_active ON "user"(is_active);
CREATE INDEX idx_categorytype_user ON categorytype(user_id);
CREATE INDEX idx_category_user ON category(user_id);
CREATE INDEX idx_category_type ON category(type_id);
CREATE INDEX idx_category_parent ON category(parent_id);
CREATE INDEX idx_recipient_user ON recipient(user_id);
CREATE INDEX idx_paymentitem_user ON paymentitem(user_id);
CREATE INDEX idx_paymentitem_date ON paymentitem(date);
CREATE INDEX idx_paymentitem_amount ON paymentitem(amount);
CREATE INDEX idx_feeplan_user ON transactionfeeplan(user_id);
CREATE INDEX idx_feerecord_payment ON transactionfeerecord(payment_item_id);
