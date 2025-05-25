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