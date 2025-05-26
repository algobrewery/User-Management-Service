-- Create user_profiles table
CREATE TABLE IF NOT EXISTS user_profiles (
    user_uuid VARCHAR(255) NOT NULL,
    organization_uuid VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    email_verification_status VARCHAR(255),
    phone VARCHAR(255) NOT NULL,
    phone_country_code INTEGER NOT NULL,
    phone_verification_status VARCHAR(255),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    status VARCHAR(255) NOT NULL,
    job_profile_uuids TEXT[],
    PRIMARY KEY (user_uuid),
    CONSTRAINT UK_user_profiles_email UNIQUE (email),
    CONSTRAINT UK_user_profiles_username UNIQUE (username)
);

-- Create job_profiles table
CREATE TABLE IF NOT EXISTS job_profiles (
    job_profile_uuid VARCHAR(255) NOT NULL,
    organization_uuid VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    reporting_manager VARCHAR(255),
    organization_unit VARCHAR(255),
    extensions_data JSONB,
    PRIMARY KEY (job_profile_uuid)
);

-- Create user_reportees table
CREATE TABLE IF NOT EXISTS user_reportees (
    relation_uuid VARCHAR(255) NOT NULL,
    organization_uuid VARCHAR(255) NOT NULL,
    manager_user_uuid VARCHAR(255) NOT NULL,
    user_uuid VARCHAR(255) NOT NULL,
    job_profile_uuid VARCHAR(255) NOT NULL,
    PRIMARY KEY (relation_uuid),
    FOREIGN KEY (user_uuid) REFERENCES user_profiles(user_uuid),
    FOREIGN KEY (job_profile_uuid) REFERENCES job_profiles(job_profile_uuid)
);

