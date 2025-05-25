CREATE TABLE IF NOT EXISTS public.user_profiles (
    user_uuid VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    email_verification_status VARCHAR(255),
    end_date TIMESTAMP WITHOUT TIME ZONE,
    first_name VARCHAR(255) NOT NULL,
    job_profile_uuids TEXT[],
    last_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255),
    organization_uuid VARCHAR(36) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    phone_country_code INTEGER NOT NULL,
    phone_verification_status VARCHAR(255),
    start_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,

    -- Unique constraints (with proper naming)
    CONSTRAINT uk_user_profiles_username UNIQUE (username),
    CONSTRAINT uk_user_profiles_email UNIQUE (email),
    CONSTRAINT uk_user_profiles_phone UNIQUE (phone)
);