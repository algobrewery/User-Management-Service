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