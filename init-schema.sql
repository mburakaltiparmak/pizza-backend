-- ============================================================================
-- DOCKER INIT SCRIPT - PostgreSQL with Test Data
-- ============================================================================
-- This script runs automatically when PostgreSQL container starts for the first time
-- ============================================================================

-- Create pizza schema
CREATE SCHEMA IF NOT EXISTS pizza;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA pizza TO postgres;

-- Set search path
SET search_path TO pizza, public;

-- ============================================================================
-- TABLES
-- ============================================================================

-- Users table
CREATE TABLE IF NOT EXISTS pizza.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    surname VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    google_id VARCHAR(255),
    supabase_id VARCHAR(255),
    oauth_provider VARCHAR(50),
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Addresses table
CREATE TABLE IF NOT EXISTS pizza.user_addresses (
    address_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    address_title VARCHAR(100) NOT NULL,
    full_address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    recipient_name VARCHAR(100),
    phone_number VARCHAR(20),
    email VARCHAR(255),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES pizza.users(id) ON DELETE CASCADE
);

-- Categories table
CREATE TABLE IF NOT EXISTS pizza.category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    img TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Products table
CREATE TABLE IF NOT EXISTS pizza.product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    rating DECIMAL(3,2) DEFAULT 0.0,
    stock INT NOT NULL DEFAULT 0,
    price DECIMAL(10,2) NOT NULL,
    img TEXT,
    category_id BIGINT,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES pizza.category(id) ON DELETE SET NULL
);

-- Orders table (with embedded delivery address)
CREATE TABLE IF NOT EXISTS pizza.orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    order_role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER',

    -- Embedded delivery address fields
    delivery_address TEXT NOT NULL,
    delivery_city VARCHAR(100) NOT NULL,
    delivery_district VARCHAR(100) NOT NULL,
    delivery_postal_code VARCHAR(20),
    delivery_phone VARCHAR(20),
    delivery_recipient_name VARCHAR(100),
    delivery_address_title VARCHAR(100),
    delivery_address_id BIGINT,
    delivery_is_default BOOLEAN,
    delivery_created_at TIMESTAMP,
    delivery_updated_at TIMESTAMP,
    email VARCHAR(255),

    guest_email VARCHAR(255),

    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    order_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2) NOT NULL,
    notes TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES pizza.users(id) ON DELETE SET NULL,
    FOREIGN KEY (delivery_address_id) REFERENCES pizza.user_addresses(address_id) ON DELETE SET NULL
);


-- Order Items table
CREATE TABLE IF NOT EXISTS pizza.order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES pizza.orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES pizza.product(id) ON DELETE RESTRICT
);

-- Payment table
CREATE TABLE IF NOT EXISTS pizza.payment (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(10,2) NOT NULL,
    transaction_id VARCHAR(255),
    error_message TEXT,
    
    -- Iyzico specific fields
    iyzico_payment_id VARCHAR(255),
    iyzico_conversation_id VARCHAR(255),
    auth_code VARCHAR(50),
    card_association VARCHAR(50),      -- VISA, MASTER_CARD, AMEX
    card_family VARCHAR(100),          -- Bonus, Axess, World
    card_bin_number VARCHAR(6),        -- First 6 digits
    card_last_four VARCHAR(4),         -- Last 4 digits
    fraud_status INTEGER,              -- 1=OK, 0=FRAUD
    installment INTEGER DEFAULT 1,
    merchant_commission_rate DECIMAL(10, 8),
    merchant_commission_amount DECIMAL(10, 2),
    iyzico_commission_rate DECIMAL(10, 8),
    iyzico_commission_fee DECIMAL(10, 2),
    merchant_payout_amount DECIMAL(10, 2),  -- Net amount after commissions
    three_ds_html_content TEXT,             -- For 3DS iframe

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES pizza.orders(id) ON DELETE CASCADE
);

-- Search Logs table (Phase 6: Search Analytics)
CREATE TABLE IF NOT EXISTS pizza.search_logs (
    id BIGSERIAL PRIMARY KEY,
    query VARCHAR(500) NOT NULL,
    result_count INTEGER NOT NULL,
    search_type VARCHAR(50),
    user_id BIGINT,
    category_id BIGINT,
    min_price DECIMAL(10,2),
    max_price DECIMAL(10,2),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    response_time_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES pizza.users(id) ON DELETE SET NULL,
    FOREIGN KEY (category_id) REFERENCES pizza.category(id) ON DELETE SET NULL
);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_users_email ON pizza.users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON pizza.users(role);
CREATE INDEX IF NOT EXISTS idx_users_status ON pizza.users(status);
CREATE INDEX IF NOT EXISTS idx_user_addresses_user_id ON pizza.user_addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_products_category_id ON pizza.product(category_id);
CREATE INDEX IF NOT EXISTS idx_products_stock ON pizza.product(stock);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON pizza.orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON pizza.orders(order_status);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON pizza.order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON pizza.order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_payment_order_id ON pizza.payment(order_id);
CREATE INDEX IF NOT EXISTS idx_search_logs_query ON pizza.search_logs(query);
CREATE INDEX IF NOT EXISTS idx_search_logs_created_at ON pizza.search_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_search_logs_category_id ON pizza.search_logs(category_id);
CREATE INDEX IF NOT EXISTS idx_payment_iyzico_payment_id ON pizza.payment(iyzico_payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_iyzico_conversation_id ON pizza.payment(iyzico_conversation_id);
CREATE INDEX IF NOT EXISTS idx_payment_auth_code ON pizza.payment(auth_code);

-- ============================================================================
-- SEQUENCES
-- ============================================================================
-- Purpose: Manual ID generation for @Embeddable entities
-- Note: BIGSERIAL already creates sequences for regular @Entity tables
-- ============================================================================

-- Address ID Sequence for user_addresses table
-- Used by UserService.generateAddressId() for @ElementCollection addresses
-- Starting from 1000 to avoid conflicts with existing test data
CREATE SEQUENCE IF NOT EXISTS pizza.user_addresses_seq
    START WITH 1000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

COMMENT ON SEQUENCE pizza.user_addresses_seq IS
    'Sequence for generating unique address_id values in user_addresses table. Used by @ElementCollection in User entity.';

-- Grant permissions to application user
GRANT USAGE, SELECT ON SEQUENCE pizza.user_addresses_seq TO postgres;

-- Initialize sequence to max existing ID + 1 (if data already exists)
-- This ensures no conflicts with existing address_id values
DO $$
DECLARE
    max_id BIGINT;
    next_val BIGINT;
BEGIN
    -- Get current max address_id
    SELECT COALESCE(MAX(address_id), 0) INTO max_id
    FROM pizza.user_addresses;

    -- Set sequence to max(current_max, 1000) + 1
    next_val := GREATEST(max_id + 1, 1000);

    PERFORM setval('pizza.user_addresses_seq', next_val, false);

    RAISE NOTICE 'Address ID sequence initialized: current_max=%, next_value=%', max_id, next_val;
END $$;
-- ============================================================================
-- TEST DATA - USERS (50 users)
-- ============================================================================

INSERT INTO pizza.users (email, password, name, surname, phone_number, role, status) VALUES
-- Admin user (password: admin123)
('admin@test.com', '$2a$10$pHELPp4xa.tjc2O50VWUw.o4M6op6z0DoBzEMzGLbZ1dAyx.wvOJm', 'Admin', 'Test', '+905551234567', 'ADMIN', 'ACTIVE'),

-- Personal user (password: personal123)
('personal@test.com', '$2a$10$9ODvwT.KRnJMGGFd/RCYlOnl8iDkk2olkJrnOOE.FYzmSzghAOlCq', 'Personal', 'Test', '+905551234568', 'PERSONAL', 'ACTIVE'),

-- Customer users (password: test123)
('customer1@test.com', '$2a$10$dmwA0/bHZMzLvSu8.3vUB.Yut7PWECludcv9r56DHw/MZ3GURd4fq', 'Ahmet', 'Yilmaz', '+905551234569', 'CUSTOMER', 'ACTIVE'),
('customer2@test.com', '$2a$10$x.V2VNa3NE2TNa7zI3fGD.RD.PLy6nz5fZHQeOUdpNSv/PFrS1ag6', 'Ayse', 'Demir', '+905551234570', 'CUSTOMER', 'ACTIVE'),
('customer3@test.com', '$2a$10$IjdK0A8CqFZo.rjOKRDpRux7VyCj6DWZSEV7SqYbN9BMLBKTh8oeu', 'Mehmet', 'Kaya', '+905551234571', 'CUSTOMER', 'ACTIVE'),
('customer4@test.com', '$2a$10$0nRep.521Uj6LRhTpL7BZuXM.w8vLdd4X0kzkJfAAQeNCnyFZW5We', 'Fatma', 'Ozturk', '+905551234572', 'CUSTOMER', 'ACTIVE'),
('customer5@test.com', '$2a$10$bteg.WqR/kXRkI0d9h68J.FlzTEuloJtl8hGLvvVXKJeyvb.brSPG', 'Ali', 'Celik', '+905551234573', 'CUSTOMER', 'ACTIVE'),
('customer6@test.com', '$2a$10$19Y0305FtUQCM2tVetzVF.qPrGGQ4YWDsVKr6PbxLA0BGSEsAmp2e', 'Zeynep', 'Arslan', '+905551234574', 'CUSTOMER', 'ACTIVE'),
-- Pending users (password: test123)
('pending1@test.com', '$2a$10$Fi9WI1nvsz5yw1lMnH0nxuhIinh2jPjqg9oJrAP/HNE8VV4wVdIAK', 'Ahmet', 'Sahin', '+905551234614', 'CUSTOMER', 'PENDING'),
('pending2@test.com', '$2a$10$AvsAF9kfrbTSUcCnDcGkEehYFC/2LEfKd5cMAFriURDuxo./Biy.q', 'Aylin', 'Ozdemir', '+905551234615', 'CUSTOMER', 'PENDING')
ON CONFLICT (email) DO NOTHING;

-- ============================================================================
-- TEST DATA - USER ADDRESSES
-- ============================================================================

INSERT INTO pizza.user_addresses (user_id, address_title, full_address, city, district, postal_code, recipient_name, phone_number, is_default) VALUES
-- Admin addresses
(1, 'Ev', 'Ataturk Caddesi No:123', 'Istanbul', 'Kadikoy', '34710', 'Admin Test', '+905551234567', TRUE),
(1, 'Is', 'Buyukdere Caddesi No:456', 'Istanbul', 'Sisli', '34394', 'Admin Test', '+905551234567', FALSE),

-- Customer 1 addresses
(3, 'Ev', 'Cumhuriyet Meydani No:78', 'Ankara', 'Cankaya', '06420', 'Ahmet Yilmaz', '+905551234569', TRUE),
(3, 'Anne Evi', 'Sanayi Mahallesi No:45', 'Ankara', 'Yenimahalle', '06200', 'Ahmet Yilmaz', '+905551234569', FALSE),

-- Customer 2 addresses
(4, 'Ev', 'Kordon Boyu No:234', 'Izmir', 'Konak', '35220', 'Ayse Demir', '+905551234570', TRUE),

-- Customer 3 addresses
(5, 'Ev', 'Lara Sahili No:567', 'Antalya', 'Muratpasa', '07100', 'Mehmet Kaya', '+905551234571', TRUE)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- TEST DATA - CATEGORIES (15 categories)
-- ============================================================================

INSERT INTO pizza.category (name, img) VALUES
('Pizza', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png'),
('Burger', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png'),
('Kizartmalar', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png'),
('Fast Food', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png'),
('Soft Drinks', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png'),
('Home Made', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png'),
('Salads', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png'),
('Pasta', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png'),
('Desserts', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png'),
('Breakfast', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png'),
('Soups', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png'),
('Wraps', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png'),
('Seafood', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png'),
('Vegan', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png'),
('Ice Cream', 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png')
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- TEST DATA - PRODUCTS (100+ products)
-- ============================================================================

INSERT INTO pizza.product (name, rating, stock, price, img, category_id, description) VALUES
-- Pizza category (ID: 1) - 20 pizzas
('Terminal Pizza', 4.9, 50, 60.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 1, 'Frische Tomaten, Italienischer Kase, Salami, Oliven, Oregano'),
('Position Absolute Pizza', 4.8, 45, 85.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 1, 'Sucuk, Sosis, Kavurma, Pastirma, Misir, Biber, Mantar'),
('useEffect Tavuklu Pizza', 4.7, 40, 75.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 1, 'Tavuk parcalari, BBQ sos, Misir, Sogan, Biber'),
('Beyaz Peynirli Pizza', 4.6, 35, 55.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 1, 'Beyaz peynir, Kasar peyniri, Domates, Biber'),
('Margarita Pizza', 4.9, 60, 50.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 1, 'Klasik Italyan pizzasi - Domates, Mozzarella, Feslegen'),
('Karisik Pizza', 4.8, 55, 70.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 1, 'Sucuk, Salam, Sosis, Misir, Mantar, Zeytin'),
('Vegetarian Pizza', 4.5, 45, 65.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 1, 'Sebzeli pizza, Mantar, Zeytin, Domates, Biber'),
('Mexican Pizza', 4.7, 40, 80.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 1, 'Jalapeno, Misir, Fasulye, Tavuk, Cheddar'),
('Four Cheese Pizza', 4.8, 35, 75.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 1, 'Mozzarella, Parmesan, Gorgonzola, Cheddar'),
('BBQ Chicken Pizza', 4.6, 50, 78.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 1, 'BBQ soslu tavuk, Sogan, Misir'),
('Pepperoni Pizza', 4.9, 55, 72.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 1, 'Klasik pepperoni pizza'),
('Hawaiian Pizza', 4.4, 30, 68.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 1, 'Ananas, Jambon, Mozzarella'),
('Seafood Pizza', 4.7, 25, 95.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 1, 'Deniz urunleri, Karides, Midye'),
('Spicy Pizza', 4.6, 40, 70.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 1, 'Aci biber, Salam, Zeytin'),
('Truffle Pizza', 4.9, 20, 120.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 1, 'Truffle mantari, Parmesan'),
('Calzone', 4.5, 35, 65.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 1, 'Kapali pizza, Ricotta peyniri'),
('Pesto Pizza', 4.7, 30, 75.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 1, 'Pesto sos, Cherry domates'),
('Meat Lovers Pizza', 4.8, 45, 90.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 1, 'Sucuk, Salam, Sosis, Dana'),
('Garden Pizza', 4.4, 50, 62.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 1, 'Taze sebzeler, Mantar'),
('Buffalo Pizza', 4.6, 40, 78.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 1, 'Buffalo soslu tavuk'),

-- Burger category (ID: 2) - 20 burgers
('Double Cheeseburger', 4.7, 30, 65.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 2, 'Iki kat kofte, Cheddar peyniri, Domates, Marul, Sogan'),
('Bacon Burger', 4.8, 25, 75.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 2, 'Dana kofte, Bacon, Barbeku sos, Marul, Tursu'),
('Chicken Burger', 4.6, 35, 55.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 2, 'Tavuk gogsu, Mayonez, Marul, Domates'),
('Classic Burger', 4.5, 40, 48.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 2, 'Dana kofte, Cheddar, Domates, Marul'),
('Mushroom Swiss Burger', 4.7, 28, 70.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 2, 'Mantar, Swiss peyniri'),
('Vegan Burger', 4.4, 35, 60.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 2, 'Bitkisel kofte, Vegan peynir'),
('Fish Burger', 4.6, 25, 58.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 2, 'Balik kofte, Tartar sos'),
('Turkey Burger', 4.5, 30, 62.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 2, 'Hindi kofte, Guacamole'),
('BBQ Burger', 4.8, 32, 68.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 2, 'BBQ soslu kofte, Sogan halkasi'),
('Jalapeno Burger', 4.6, 28, 65.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 2, 'Aci jalapeno, Cheddar'),
('Blue Cheese Burger', 4.7, 22, 75.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 2, 'Blue cheese, Karamelize sogan'),
('Avocado Burger', 4.5, 30, 70.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 2, 'Taze avokado, Lime'),
('Egg Burger', 4.6, 35, 58.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 2, 'Yumurta, Bacon'),
('Crispy Chicken Burger', 4.7, 40, 62.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 2, 'Citir tavuk, Ranch sos'),
('Lamb Burger', 4.8, 20, 85.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 2, 'Kuzu eti, Feta peyniri'),
('Pulled Pork Burger', 4.6, 25, 72.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 2, 'YavaÅŸ pisirmis domuz eti'),
('Teriyaki Burger', 4.5, 30, 68.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 2, 'Teriyaki soslu kofte'),
('Slider Trio', 4.7, 35, 55.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 2, 'Uc mini burger'),
('Giant Burger', 4.9, 15, 95.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 2, 'Uc katli dev burger'),
('Breakfast Burger', 4.4, 25, 58.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 2, 'Yumurta, Bacon, Hash brown'),

-- Kizartmalar (ID: 3) - 15 items
('Patates Kizartmasi', 4.5, 100, 25.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 3, 'Klasik patates kizartmasi'),
('Sogan Halkasi', 4.4, 80, 30.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 3, 'Citir sogan halkalari'),
('Mozzarella Stick', 4.6, 60, 35.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 3, 'Kizarmis mozzarella'),
('Chicken Wings', 4.7, 70, 42.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 3, 'Citir tavuk kanatlari'),
('Sweet Potato Fries', 4.5, 50, 32.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 3, 'Tatli patates kizartmasi'),
('Calamari', 4.6, 40, 48.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 3, 'Kalamar halkasi'),
('Jalapeno Poppers', 4.5, 55, 38.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 3, 'Peynirli jalapeno'),
('Loaded Fries', 4.8, 45, 42.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 3, 'Cheddar, bacon, ranch'),
('Zucchini Fries', 4.4, 35, 32.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 3, 'Kabak kizartmasi'),
('Cheese Bites', 4.6, 60, 36.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 3, 'Peynir toplarÄ±'),
('Corn Fritters', 4.5, 40, 28.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 3, 'Misir koftesi'),
('Pickle Fries', 4.3, 30, 30.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 3, 'Kizarmis tursu'),
('Mac & Cheese Bites', 4.7, 50, 38.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 3, 'Makarna peynir toplarÄ±'),
('Buffalo Cauliflower', 4.5, 35, 34.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 3, 'Buffalo soslu karnabahar'),
('Tempura Vegetables', 4.6, 40, 40.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 3, 'Tempura sebze'),

-- Fast Food (ID: 4) - 10 items
('Nugget', 4.5, 70, 40.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 4, '10 parca tavuk nugget'),
('Hot Dog', 4.3, 50, 35.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 4, 'Klasik hot dog'),
('Corn Dog', 4.4, 45, 32.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 4, 'Misirli sosis'),
('Tacos', 4.6, 60, 45.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 4, 'Mexican taco'),
('Burrito', 4.7, 40, 55.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 4, 'Tavuk burrito'),
('Quesadilla', 4.5, 50, 48.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 4, 'Peynirli quesadilla'),
('Nachos', 4.6, 55, 42.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 4, 'Peynirli nachos'),
('Falafel', 4.4, 35, 38.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 4, 'Nohut koftesi'),
('Spring Rolls', 4.5, 40, 36.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 4, 'Bahar rulo'),
('Samosa', 4.3, 45, 32.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 4, 'Patatesli borek'),

-- Soft Drinks (ID: 5) - 10 items
('Coca Cola', 4.8, 200, 15.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 5, '330ml Coca Cola'),
('Fanta', 4.7, 180, 15.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 5, '330ml Fanta'),
('Sprite', 4.7, 175, 15.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 5, '330ml Sprite'),
('Ayran', 4.9, 150, 10.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 5, 'Ev yapimi ayran'),
('Ice Tea', 4.6, 160, 18.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 5, 'Seftali ice tea'),
('Energy Drink', 4.5, 120, 25.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 5, 'Energy drink'),
('Orange Juice', 4.7, 90, 22.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 5, 'Taze portakal suyu'),
('Apple Juice', 4.6, 85, 20.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 5, 'Elma suyu'),
('Sparkling Water', 4.4, 140, 12.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 5, 'Maden suyu'),
('Lemonade', 4.8, 100, 18.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 5, 'Limonata'),

-- Home Made (ID: 6) - 8 items
('Ev Yapimi Limonata', 4.9, 40, 20.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 6, 'Taze sikilmis limon'),
('Kurabiye', 4.7, 30, 25.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 6, 'Cikolata kurabiye'),
('Brownie', 4.8, 35, 28.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 6, 'Ev yapimi brownie'),
('Cheesecake', 4.9, 25, 35.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 6, 'Cilekli cheesecake'),
('Muffin', 4.6, 40, 22.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 6, 'Yaban mersinli muffin'),
('Apple Pie', 4.8, 20, 32.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 6, 'Elmali turta'),
('Tiramisu', 4.9, 18, 38.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 6, 'Klasik tiramisu'),
('Profiterole', 4.7, 30, 42.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 6, 'Cikolata soslu profiterol'),

-- Salads (ID: 7) - 8 items
('Caesar Salad', 4.7, 45, 38.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 7, 'Tavuklu Caesar salata'),
('Greek Salad', 4.6, 50, 35.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 7, 'Yunan salatasi'),
('Garden Salad', 4.5, 60, 28.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 7, 'Mevsim salatasi'),
('Tuna Salad', 4.6, 35, 42.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 7, 'Ton balikli salata'),
('Quinoa Salad', 4.7, 40, 45.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 7, 'Kinoa salatasi'),
('Caprese Salad', 4.8, 30, 40.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-1_d2v45l.png', 7, 'Mozzarella, domates, feslegen'),
('Cobb Salad', 4.6, 35, 48.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png', 7, 'Tavuk, bacon, avokado'),
('Asian Salad', 4.5, 40, 42.00, 'https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-3_ksyhvw.png', 7, 'Asya usulu salata')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- TEST DATA - ORDERS (Realistic test orders with all fields)
-- ============================================================================

-- Test Order 1: Customer 1 (Ahmet Yilmaz) - Using saved address - PENDING
INSERT INTO pizza.orders (
    user_id,
    order_role,
    delivery_address,
    delivery_city,
    delivery_district,
    delivery_postal_code,
    delivery_phone,
    delivery_recipient_name,
    delivery_address_title,
    delivery_address_id,
    delivery_is_default,
    guest_email,
    order_date,
    order_status,
    total_amount,
    notes
) VALUES (
    3, -- customer1@test.com (Ahmet Yilmaz)
    'CUSTOMER',
    'Cumhuriyet Meydani No:78',
    'Ankara',
    'Cankaya',
    '06420',
    '+905551234569',
    'Ahmet Yilmaz',
    'Ev',
    3, -- delivery_address_id (customer1's home address)
    TRUE,
    NULL, -- Not a guest order
    CURRENT_TIMESTAMP - INTERVAL '2 hours',
    'PENDING',
    145.00,
    'Lutfen kapiyi calmayin, zili kullanin'
);

-- Test Order 2: Customer 2 (Ayse Demir) - Using saved address - CONFIRMED
INSERT INTO pizza.orders (
    user_id,
    order_role,
    delivery_address,
    delivery_city,
    delivery_district,
    delivery_postal_code,
    delivery_phone,
    delivery_recipient_name,
    delivery_address_title,
    delivery_address_id,
    delivery_is_default,
    guest_email,
    order_date,
    order_status,
    total_amount,
    notes
) VALUES (
    4, -- customer2@test.com (Ayse Demir)
    'CUSTOMER',
    'Kordon Boyu No:234',
    'Izmir',
    'Konak',
    '35220',
    '+905551234570',
    'Ayse Demir',
    'Ev',
    5, -- delivery_address_id
    TRUE,
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '1 hour 30 minutes',
    'CONFIRMED',
    220.00,
    'Ekstra sos lutfen'
);

-- Test Order 3: Customer 3 (Mehmet Kaya) - Using saved address - PREPARING
INSERT INTO pizza.orders (
    user_id,
    order_role,
    delivery_address,
    delivery_city,
    delivery_district,
    delivery_postal_code,
    delivery_phone,
    delivery_recipient_name,
    delivery_address_title,
    delivery_address_id,
    delivery_is_default,
    guest_email,
    order_date,
    order_status,
    total_amount,
    notes
) VALUES (
    5, -- customer3@test.com (Mehmet Kaya)
    'CUSTOMER',
    'Lara Sahili No:567',
    'Antalya',
    'Muratpasa',
    '07100',
    '+905551234571',
    'Mehmet Kaya',
    'Ev',
    6, -- delivery_address_id
    TRUE,
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '45 minutes',
    'PREPARING',
    180.00,
    NULL
);

-- Test Order 4: Customer 1 (Ahmet Yilmaz) - Alternative address - ON_THE_WAY
INSERT INTO pizza.orders (
    user_id,
    order_role,
    delivery_address,
    delivery_city,
    delivery_district,
    delivery_postal_code,
    delivery_phone,
    delivery_recipient_name,
    delivery_address_title,
    delivery_address_id,
    delivery_is_default,
    guest_email,
    order_date,
    order_status,
    total_amount,
    notes
) VALUES (
    3, -- customer1@test.com
    'CUSTOMER',
    'Sanayi Mahallesi No:45',
    'Ankara',
    'Yenimahalle',
    '06200',
    '+905551234569',
    'Ahmet Yilmaz',
    'Anne Evi',
    4, -- delivery_address_id (alternative address)
    FALSE,
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '30 minutes',
    'SHIPPING',
    95.00,
    'Annemin evine gonderiyorum'
);

-- Test Order 5: GUEST Order - New address provided - PENDING
INSERT INTO pizza.orders (
    user_id,
    order_role,
    delivery_address,
    delivery_city,
    delivery_district,
    delivery_postal_code,
    delivery_phone,
    delivery_recipient_name,
    delivery_address_title,
    delivery_address_id,
    delivery_is_default,
    guest_email,
    order_date,
    order_status,
    total_amount,
    notes
) VALUES (
    NULL, -- Guest order
    'GUEST',
    'Bahcelievler Mah. 5. Cadde No:89',
    'Istanbul',
    'Bahcelievler',
    '34180',
    '+905559876543',
    'Zeynep Aksoy',
    'Ev',
    NULL, -- No saved address
    NULL,
    'guest.user@example.com',
    CURRENT_TIMESTAMP - INTERVAL '10 minutes',
    'PENDING',
    125.00,
    'Ilk siparis, lutfen dikkatli olun'
);

-- Test Order 6: Customer 2 (Ayse Demir) - DELIVERED (Old order)
INSERT INTO pizza.orders (
    user_id,
    order_role,
    delivery_address,
    delivery_city,
    delivery_district,
    delivery_postal_code,
    delivery_phone,
    delivery_recipient_name,
    delivery_address_title,
    delivery_address_id,
    delivery_is_default,
    guest_email,
    order_date,
    order_status,
    total_amount,
    notes
) VALUES (
    4, -- customer2@test.com
    'CUSTOMER',
    'Kordon Boyu No:234',
    'Izmir',
    'Konak',
    '35220',
    '+905551234570',
    'Ayse Demir',
    'Ev',
    5,
    TRUE,
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '2 days',
    'DELIVERED',
    340.00,
    'Cok tesekkurler, harika!'
);

-- Test Order 7: Customer 3 (Mehmet Kaya) - CANCELLED
INSERT INTO pizza.orders (
    user_id,
    order_role,
    delivery_address,
    delivery_city,
    delivery_district,
    delivery_postal_code,
    delivery_phone,
    delivery_recipient_name,
    delivery_address_title,
    delivery_address_id,
    delivery_is_default,
    guest_email,
    order_date,
    order_status,
    total_amount,
    notes
) VALUES (
    5, -- customer3@test.com
    'CUSTOMER',
    'Lara Sahili No:567',
    'Antalya',
    'Muratpasa',
    '07100',
    '+905551234571',
    'Mehmet Kaya',
    'Ev',
    6,
    TRUE,
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '3 hours',
    'CANCELLED',
    75.00,
    'Yanlislikla siparis verdim'
);

-- Test Order 8: GUEST Order - CONFIRMED
INSERT INTO pizza.orders (
    user_id,
    order_role,
    delivery_address,
    delivery_city,
    delivery_district,
    delivery_postal_code,
    delivery_phone,
    delivery_recipient_name,
    delivery_address_title,
    delivery_address_id,
    delivery_is_default,
    guest_email,
    order_date,
    order_status,
    total_amount,
    notes
) VALUES (
    NULL,
    'GUEST',
    'Atakent Mah. Palmiye Sok. No:12',
    'Antalya',
    'Konyaalti',
    '07070',
    '+905551112233',
    'Emre Yildirim',
    NULL,
    NULL,
    NULL,
    'emre.yildirim@example.com',
    CURRENT_TIMESTAMP - INTERVAL '1 hour',
    'CONFIRMED',
    265.00,
    NULL
);

-- Test Order 9: Admin user test order - DELIVERED
INSERT INTO pizza.orders (
    user_id,
    order_role,
    delivery_address,
    delivery_city,
    delivery_district,
    delivery_postal_code,
    delivery_phone,
    delivery_recipient_name,
    delivery_address_title,
    delivery_address_id,
    delivery_is_default,
    guest_email,
    order_date,
    order_status,
    total_amount,
    notes
) VALUES (
    1, -- admin@test.com
    'CUSTOMER',
    'Ataturk Caddesi No:123',
    'Istanbul',
    'Kadikoy',
    '34710',
    '+905551234567',
    'Admin Test',
    'Ev',
    1,
    TRUE,
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    'DELIVERED',
    450.00,
    'Ofis partisi icin siparis'
);

-- Test Order 10: Customer 1 - Large order - PREPARING
INSERT INTO pizza.orders (
    user_id,
    order_role,
    delivery_address,
    delivery_city,
    delivery_district,
    delivery_postal_code,
    delivery_phone,
    delivery_recipient_name,
    delivery_address_title,
    delivery_address_id,
    delivery_is_default,
    guest_email,
    order_date,
    order_status,
    total_amount,
    notes
) VALUES (
    3, -- customer1@test.com
    'CUSTOMER',
    'Cumhuriyet Meydani No:78',
    'Ankara',
    'Cankaya',
    '06420',
    '+905551234569',
    'Ahmet Yilmaz',
    'Ev',
    3,
    TRUE,
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '25 minutes',
    'PREPARING',
    580.00,
    'Dogum gunu partisi - 20 kisi'
);

-- ============================================================================
-- TEST DATA - ORDER ITEMS (Items for each order)
-- ============================================================================

-- Order 1 items (ID: 1, Total: 145.00)
INSERT INTO pizza.order_items (order_id, product_id, quantity, price) VALUES
(1, 1, 2, 60.00),  -- 2x Terminal Pizza
(1, 41, 1, 25.00); -- 1x Patates Kizartmasi

-- Order 2 items (ID: 2, Total: 220.00)
INSERT INTO pizza.order_items (order_id, product_id, quantity, price) VALUES
(2, 2, 1, 85.00),  -- 1x Position Absolute Pizza
(2, 3, 1, 75.00),  -- 1x useEffect Tavuklu Pizza
(2, 1, 1, 60.00);  -- 1x Terminal Pizza

-- Order 3 items (ID: 3, Total: 180.00)
INSERT INTO pizza.order_items (order_id, product_id, quantity, price) VALUES
(3, 9, 2, 75.00),  -- 2x Four Cheese Pizza
(3, 42, 1, 30.00); -- 1x Sogan Halkasi

-- Order 4 items (ID: 4, Total: 95.00)
INSERT INTO pizza.order_items (order_id, product_id, quantity, price) VALUES
(4, 5, 1, 50.00),  -- 1x Margarita Pizza
(4, 21, 1, 65.00); -- 1x Double Cheeseburger
-- Note: Total should be 115, but we set 95 to test price validation

-- Order 5 items (ID: 5, Total: 125.00) - Guest order
INSERT INTO pizza.order_items (order_id, product_id, quantity, price) VALUES
(5, 6, 1, 70.00),  -- 1x Karisik Pizza
(5, 61, 2, 15.00), -- 2x Coca Cola
(5, 41, 1, 25.00); -- 1x Patates Kizartmasi

-- Order 6 items (ID: 6, Total: 340.00) - Delivered
INSERT INTO pizza.order_items (order_id, product_id, quantity, price) VALUES
(6, 15, 2, 120.00), -- 2x Truffle Pizza
(6, 21, 1, 65.00),  -- 1x Double Cheeseburger
(6, 44, 1, 42.00),  -- 1x Chicken Wings
(6, 68, 2, 35.00);  -- 2x Cheesecake

-- Order 7 items (ID: 7, Total: 75.00) - Cancelled
INSERT INTO pizza.order_items (order_id, product_id, quantity, price) VALUES
(7, 3, 1, 75.00);   -- 1x useEffect Tavuklu Pizza

-- Order 8 items (ID: 8, Total: 265.00) - Guest confirmed
INSERT INTO pizza.order_items (order_id, product_id, quantity, price) VALUES
(8, 18, 2, 90.00),  -- 2x Meat Lovers Pizza
(8, 22, 1, 75.00),  -- 1x Bacon Burger
(8, 41, 1, 25.00);  -- 1x Patates Kizartmasi

-- Order 9 items (ID: 9, Total: 450.00) - Admin delivered
INSERT INTO pizza.order_items (order_id, product_id, quantity, price) VALUES
(9, 1, 3, 60.00),   -- 3x Terminal Pizza
(9, 2, 2, 85.00),   -- 2x Position Absolute Pizza
(9, 21, 2, 65.00),  -- 2x Double Cheeseburger
(9, 41, 4, 25.00),  -- 4x Patates Kizartmasi
(9, 61, 6, 15.00);  -- 6x Coca Cola

-- Order 10 items (ID: 10, Total: 580.00) - Large party order
INSERT INTO pizza.order_items (order_id, product_id, quantity, price) VALUES
(10, 1, 4, 60.00),  -- 4x Terminal Pizza
(10, 5, 4, 50.00),  -- 4x Margarita Pizza
(10, 11, 2, 72.00), -- 2x Pepperoni Pizza
(10, 21, 4, 65.00), -- 4x Double Cheeseburger
(10, 41, 6, 25.00), -- 6x Patates Kizartmasi
(10, 61, 10, 15.00),-- 10x Coca Cola
(10, 64, 2, 18.00); -- 2x Ice Tea

-- ============================================================================
-- TEST DATA - PAYMENTS (Payment for each order)
-- ============================================================================

-- Payment 1: Order 1 - PENDING (Cash on delivery)
INSERT INTO pizza.payment (order_id, payment_method, payment_status, amount, transaction_id, error_message, created_at, completed_at) VALUES
(1, 'CASH', 'PENDING', 145.00, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '2 hours', NULL);

-- Payment 2: Order 2 - SUCCESS (Online credit card)
INSERT INTO pizza.payment (order_id, payment_method, payment_status, amount, transaction_id, error_message, created_at, completed_at) VALUES
(2, 'ONLINE_CREDIT_CARD', 'SUCCESS', 220.00, 'TXN_20251122001', NULL, CURRENT_TIMESTAMP - INTERVAL '1 hour 30 minutes', CURRENT_TIMESTAMP - INTERVAL '1 hour 29 minutes');

-- Payment 3: Order 3 - PENDING (Credit card at door)
INSERT INTO pizza.payment (order_id, payment_method, payment_status, amount, transaction_id, error_message, created_at, completed_at) VALUES
(3, 'CREDIT_CARD', 'PENDING', 180.00, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '45 minutes', NULL);

-- Payment 4: Order 4 - SUCCESS (Cash)
INSERT INTO pizza.payment (order_id, payment_method, payment_status, amount, transaction_id, error_message, created_at, completed_at) VALUES
(4, 'CASH', 'SUCCESS', 95.00, 'CASH_20251122002', NULL, CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '15 minutes');

-- Payment 5: Order 5 - PENDING (Guest - Cash)
INSERT INTO pizza.payment (order_id, payment_method, payment_status, amount, transaction_id, error_message, created_at, completed_at) VALUES
(5, 'CASH', 'PENDING', 125.00, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '10 minutes', NULL);

-- Payment 6: Order 6 - SUCCESS (Delivered - Gift card)
INSERT INTO pizza.payment (order_id, payment_method, payment_status, amount, transaction_id, error_message, created_at, completed_at) VALUES
(6, 'GIFT_CARD', 'SUCCESS', 340.00, 'GIFT_20251120001', NULL, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '5 minutes');

-- Payment 7: Order 7 - REFUNDED (Cancelled order)
INSERT INTO pizza.payment (order_id, payment_method, payment_status, amount, transaction_id, error_message, created_at, completed_at) VALUES
(7, 'ONLINE_CREDIT_CARD', 'REFUNDED', 75.00, 'TXN_20251122003_REFUND', NULL, CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours 45 minutes');

-- Payment 8: Order 8 - SUCCESS (Guest confirmed - Online)
INSERT INTO pizza.payment (order_id, payment_method, payment_status, amount, transaction_id, error_message, created_at, completed_at) VALUES
(8, 'ONLINE_CREDIT_CARD', 'SUCCESS', 265.00, 'TXN_20251122004', NULL, CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '59 minutes');

-- Payment 9: Order 9 - SUCCESS (Admin - Credit card)
INSERT INTO pizza.payment (order_id, payment_method, payment_status, amount, transaction_id, error_message, created_at, completed_at) VALUES
(9, 'CREDIT_CARD', 'SUCCESS', 450.00, 'TXN_20251121001', NULL, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '2 hours');

-- Payment 10: Order 10 - PENDING (Large order - Cash)
INSERT INTO pizza.payment (order_id, payment_method, payment_status, amount, transaction_id, error_message, created_at, completed_at) VALUES
(10, 'CASH', 'PENDING', 580.00, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '25 minutes', NULL);

-- ============================================================================
-- TEST DATA SUMMARY
-- ============================================================================

DO $$
DECLARE
    order_count INT;
    item_count INT;
    payment_count INT;
BEGIN
    SELECT COUNT(*) INTO order_count FROM pizza.orders;
    SELECT COUNT(*) INTO item_count FROM pizza.order_items;
    SELECT COUNT(*) INTO payment_count FROM pizza.payment;

    RAISE NOTICE '================================================';
    RAISE NOTICE 'TEST DATA INSERTION COMPLETE';
    RAISE NOTICE '================================================';
    RAISE NOTICE 'Orders Created: %', order_count;
    RAISE NOTICE 'Order Items: %', item_count;
    RAISE NOTICE 'Payments: %', payment_count;
    RAISE NOTICE '';
    RAISE NOTICE 'Order Status Distribution:';
    RAISE NOTICE '  - PENDING: 3 orders';
    RAISE NOTICE '  - CONFIRMED: 2 orders';
    RAISE NOTICE '  - PREPARING: 2 orders';
    RAISE NOTICE '  - ON_THE_WAY: 1 order';
    RAISE NOTICE '  - DELIVERED: 2 orders';
    RAISE NOTICE '  - CANCELLED: 1 order';
    RAISE NOTICE '';
    RAISE NOTICE 'Order Types:';
    RAISE NOTICE '  - Customer Orders: 8';
    RAISE NOTICE '  - Guest Orders: 2';
    RAISE NOTICE '';
    RAISE NOTICE 'Payment Methods:';
    RAISE NOTICE '  - CASH: 4 orders';
    RAISE NOTICE '  - CREDIT_CARD: 2 orders';
    RAISE NOTICE '  - ONLINE_CREDIT_CARD: 3 orders';
    RAISE NOTICE '  - GIFT_CARD: 1 order';
    RAISE NOTICE '================================================';
END $$;

-- ============================================================================
-- SUCCESS MESSAGE
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE '================================================';
    RAISE NOTICE 'Pizza schema initialized with test data!';
    RAISE NOTICE 'Users: 10 | Categories: 15 | Products: 109';
    RAISE NOTICE 'Orders: 10 | Order Items: 44 | Payments: 10';
    RAISE NOTICE 'Perfect for testing pagination & Redis cache!';
    RAISE NOTICE '================================================';
END $$;

-- ============================================================================
-- PHASE 4.4: JWT Refresh Token Flow - Database Migration
-- ============================================================================
-- Author: Burak AltÄ±parmak
-- Date: 14 KasÄ±m 2025
-- Version: 4.4.0
--
-- Purpose: Create refresh_tokens table for JWT refresh token management
--
-- Features:
-- - Token storage with expiration tracking
-- - User relationship (One user -> Many tokens)
-- - Revocation support for security
-- - Audit trail (created_at, last_used_at)
-- - Device tracking (user_agent, ip_address)
-- - Performance indexes
-- ============================================================================

-- Drop table if exists (for clean migration)
DROP TABLE IF EXISTS pizza.refresh_tokens CASCADE;

-- Create refresh_tokens table
CREATE TABLE pizza.refresh_tokens (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,

    -- Token string (UUID format, unique)
    token VARCHAR(255) NOT NULL UNIQUE,

    -- User relationship
    user_id BIGINT NOT NULL,

    -- Token lifecycle
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,

    -- Audit trail
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,

    -- Device tracking (optional, for security)
    user_agent VARCHAR(500),
    ip_address VARCHAR(50),

    -- Foreign key constraint
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
        REFERENCES pizza.users(id)
        ON DELETE CASCADE
);

-- ============================================================================
-- INDEXES for Performance
-- ============================================================================

-- Index on token (most frequent lookup)
CREATE INDEX idx_refresh_tokens_token
ON pizza.refresh_tokens(token);

-- Index on user_id (for finding all user tokens)
CREATE INDEX idx_refresh_tokens_user_id
ON pizza.refresh_tokens(user_id);

-- Index on expiry_date (for cleanup job)
CREATE INDEX idx_refresh_tokens_expiry_date
ON pizza.refresh_tokens(expiry_date);

-- Composite index for valid token queries
CREATE INDEX idx_refresh_tokens_valid
ON pizza.refresh_tokens(user_id, revoked, expiry_date);

-- ============================================================================
-- COMMENTS for Documentation
-- ============================================================================

COMMENT ON TABLE pizza.refresh_tokens IS
'Stores JWT refresh tokens for long-term authentication. Supports token rotation, revocation, and cleanup.';

COMMENT ON COLUMN pizza.refresh_tokens.token IS
'UUID-based refresh token string. Unique identifier for token validation.';

COMMENT ON COLUMN pizza.refresh_tokens.user_id IS
'Foreign key to users table. One user can have multiple refresh tokens (multiple devices/sessions).';

COMMENT ON COLUMN pizza.refresh_tokens.expiry_date IS
'Token expiration timestamp (UTC). Tokens expire after 7-30 days depending on configuration.';

COMMENT ON COLUMN pizza.refresh_tokens.revoked IS
'Revocation flag. True if token was manually revoked (logout, password change, security breach).';

COMMENT ON COLUMN pizza.refresh_tokens.created_at IS
'Token creation timestamp. Auto-populated on insert.';

COMMENT ON COLUMN pizza.refresh_tokens.last_used_at IS
'Last time token was used for refresh. Updated on each successful refresh request.';

COMMENT ON COLUMN pizza.refresh_tokens.user_agent IS
'Browser/device user agent string. Used for security monitoring and displaying active sessions.';

COMMENT ON COLUMN pizza.refresh_tokens.ip_address IS
'Client IP address. Used for security monitoring and detecting suspicious activity.';

-- ============================================================================
-- VERIFICATION
-- ============================================================================

-- Verify table creation
SELECT
    table_schema,
    table_name,
    table_type
FROM information_schema.tables
WHERE table_schema = 'pizza'
AND table_name = 'refresh_tokens';

-- Verify indexes
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'pizza'
AND tablename = 'refresh_tokens'
ORDER BY indexname;

-- ============================================================================
-- USAGE EXAMPLES
-- ============================================================================

-- Example 1: Insert a refresh token
-- INSERT INTO pizza.refresh_tokens (token, user_id, expiry_date)
-- VALUES (
--     'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
--     1,
--     CURRENT_TIMESTAMP + INTERVAL '30 days'
-- );

-- Example 2: Find valid tokens for a user
-- SELECT * FROM pizza.refresh_tokens
-- WHERE user_id = 1
-- AND revoked = false
-- AND expiry_date > CURRENT_TIMESTAMP;

-- Example 3: Revoke all tokens for a user
-- UPDATE pizza.refresh_tokens
-- SET revoked = true
-- WHERE user_id = 1;

-- Example 4: Delete expired tokens (cleanup)
-- DELETE FROM pizza.refresh_tokens
-- WHERE expiry_date < CURRENT_TIMESTAMP;

-- ============================================================================
-- ROLLBACK (if needed)
-- ============================================================================

-- To rollback this migration:
-- DROP TABLE IF EXISTS pizza.refresh_tokens CASCADE;

-- Add this section at the END of init-schema.sql file
-- AFTER the refresh_tokens ROLLBACK comment
-- Replace the last section starting from "-- To rollback this migration:"

-- ============================================================================
-- ROLLBACK (if needed)
-- ============================================================================

-- To rollback refresh_tokens migration:
-- DROP TABLE IF EXISTS pizza.refresh_tokens CASCADE;

-- ============================================================================
-- PHASE 4.5: VERIFICATION TOKENS TABLE - Email Verification & Password Reset
-- ============================================================================
-- Author: Burak AltÄ±parmak
-- Date: 17 KasÄ±m 2025
-- Version: 4.5.1
--
-- Purpose: Store email verification and password reset tokens
--
-- Features:
-- - Email verification tokens (24 hours expiry)
-- - Password reset tokens (1 hour expiry)
-- - User relationship (One user -> Many tokens)
-- - Automatic cleanup via expiry_date
-- - Performance indexes for token lookup
-- ============================================================================

-- Create verification_tokens table
CREATE TABLE IF NOT EXISTS pizza.verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,  -- â† YENÄ°
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verification_token_user
        FOREIGN KEY (user_id)
        REFERENCES pizza.users(id)
        ON DELETE CASCADE
);

-- ============================================================================
-- INDEXES for Performance
-- ============================================================================

-- Index on token (most frequent lookup)
CREATE INDEX IF NOT EXISTS idx_verification_tokens_token
ON pizza.verification_tokens(token);

-- Index on user_id (for finding user tokens)
CREATE INDEX IF NOT EXISTS idx_verification_tokens_user_id
ON pizza.verification_tokens(user_id);

-- Index on expiry_date (for cleanup job)
CREATE INDEX IF NOT EXISTS idx_verification_tokens_expiry_date
ON pizza.verification_tokens(expiry_date);

-- ============================================================================
-- COMMENTS for Documentation
-- ============================================================================

COMMENT ON TABLE pizza.verification_tokens IS
'Stores email verification and password reset tokens with expiration tracking';

COMMENT ON COLUMN pizza.verification_tokens.token IS
'UUID token for email verification or password reset';

COMMENT ON COLUMN pizza.verification_tokens.user_id IS
'Foreign key to users table';

COMMENT ON COLUMN pizza.verification_tokens.expiry_date IS
'Token expiration timestamp (24 hours for verification, 1 hour for reset)';

-- ============================================================================
-- VERIFICATION
-- ============================================================================

-- Verify table creation
SELECT
    table_schema,
    table_name,
    table_type
FROM information_schema.tables
WHERE table_schema = 'pizza'
AND table_name = 'verification_tokens';

-- Verify indexes
SELECT
    schemaname,
    tablename,
    indexname
FROM pg_indexes
WHERE schemaname = 'pizza'
AND tablename = 'verification_tokens'
ORDER BY indexname;

-- ============================================================================
-- SUCCESS MESSAGE (Phase 4.5)
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE '================================================';
    RAISE NOTICE 'Verification tokens table created successfully!';
    RAISE NOTICE 'Table: pizza.verification_tokens';
    RAISE NOTICE 'Indexes: 3 (token, user_id, expiry_date)';
    RAISE NOTICE '================================================';
END $$;

-- ============================================================================
-- ROLLBACK (if needed)
-- ============================================================================

-- To rollback verification_tokens migration:
-- DROP TABLE IF EXISTS pizza.verification_tokens CASCADE;

