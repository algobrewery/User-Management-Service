-- Production Database Setup Script
-- Run this script to set up the production database schema

-- Create user_profiles table
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

-- Create job_profiles table
CREATE TABLE IF NOT EXISTS public.job_profiles (
    job_profile_uuid VARCHAR(36) PRIMARY KEY,
    end_date TIMESTAMP WITHOUT TIME ZONE,
    extensions_data JSONB,
    organization_unit VARCHAR(255),
    organization_uuid VARCHAR(36) NOT NULL,
    reporting_manager VARCHAR(36),
    start_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    title VARCHAR(255) NOT NULL,

    -- Foreign key to validate reporting_manager exists in user_profiles
    CONSTRAINT fk_job_profiles_reporting_manager
        FOREIGN KEY (reporting_manager, organization_uuid)
        REFERENCES public.user_profiles(user_uuid, organization_uuid)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- Create user_reportees table
CREATE TABLE IF NOT EXISTS public.user_reportees (
    relation_uuid VARCHAR(36) PRIMARY KEY,
    job_profile_uuid VARCHAR(36) NOT NULL,
    manager_user_uuid VARCHAR(36) NOT NULL,
    organization_uuid VARCHAR(36) NOT NULL,
    user_uuid VARCHAR(36) NOT NULL,

    -- Foreign key to validate manager_user_uuid exists in user_profiles
    CONSTRAINT fk_manager_user_profile
        FOREIGN KEY (manager_user_uuid, organization_uuid)
        REFERENCES public.user_profiles(user_uuid, organization_uuid)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    -- Foreign key to validate job_profile_uuid exists in job_profiles
    CONSTRAINT fk_job_profile
        FOREIGN KEY (job_profile_uuid, organization_uuid)
        REFERENCES public.job_profiles(job_profile_uuid, organization_uuid)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_user_profiles_organization_uuid ON public.user_profiles(organization_uuid);
CREATE INDEX IF NOT EXISTS idx_user_profiles_status ON public.user_profiles(status);
CREATE INDEX IF NOT EXISTS idx_job_profiles_organization_uuid ON public.job_profiles(organization_uuid);
CREATE INDEX IF NOT EXISTS idx_job_profiles_reporting_manager ON public.job_profiles(reporting_manager);
CREATE INDEX IF NOT EXISTS idx_user_reportees_organization_uuid ON public.user_reportees(organization_uuid);
CREATE INDEX IF NOT EXISTS idx_user_reportees_manager_user_uuid ON public.user_reportees(manager_user_uuid);

-- Insert some sample data (optional)
-- You can remove this section if you don't want sample data

-- Sample organization UUID
-- INSERT INTO public.user_profiles (user_uuid, email, email_verification_status, first_name, last_name, organization_uuid, phone, phone_country_code, phone_verification_status, start_date, status, username) 
-- VALUES ('sample-user-uuid', 'admin@example.com', 'VERIFIED', 'Admin', 'User', 'sample-org-uuid', '1234567890', 1, 'VERIFIED', NOW(), 'ACTIVE', 'admin');

COMMIT;
