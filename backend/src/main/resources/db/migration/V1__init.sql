CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Users table
CREATE TABLE IF NOT EXISTS app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(30) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    date_of_birth DATE,
    security_question VARCHAR(255),
    security_answer_hash VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    is_volunteer BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN NOT NULL DEFAULT TRUE,
    volunteer_rating NUMERIC(3,2),
    total_helped INTEGER NOT NULL DEFAULT 0,
    thank_points_total INTEGER NOT NULL DEFAULT 0,
    completion_rate NUMERIC(5,2),
    avg_response_time VARCHAR(50),
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Emergencies table
CREATE TABLE IF NOT EXISTS emergencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    user_name VARCHAR(200) NOT NULL,
    user_phone VARCHAR(30),
    type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description TEXT,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    address VARCHAR(500) NOT NULL,
    volunteers INTEGER NOT NULL DEFAULT 0,
    responding_volunteer_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    volunteer_phone VARCHAR(30),
    requester_ip VARCHAR(80),
    response_time_min DOUBLE PRECISION,
    thank_points INTEGER CHECK (thank_points IS NULL OR (thank_points >= 1 AND thank_points <= 5)),
    thanked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_emergency_status_created_at ON emergencies(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_emergency_created_at ON emergencies(created_at DESC);

-- Session management
CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(128) NOT NULL UNIQUE,
    user_agent VARCHAR(300),
    ip_address VARCHAR(80),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_user ON user_sessions(user_id);

-- Token revocation (for logout)
CREATE TABLE IF NOT EXISTS revoked_access_tokens (
    jti VARCHAR(80) PRIMARY KEY,
    user_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
