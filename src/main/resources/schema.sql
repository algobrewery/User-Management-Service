-- Create user_profiles table
CREATE TABLE IF NOT EXISTS user_profiles (
    user_uuid VARCHAR(255) PRIMARY KEY,
    organization_uuid VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255),
    last_name VARCHAR(255) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    job_profile_uuids VARCHAR(255) ARRAY,
    email VARCHAR(255) NOT NULL UNIQUE,
    email_verification_status VARCHAR(50),
    phone VARCHAR(20) NOT NULL UNIQUE,
    phone_country_code INTEGER NOT NULL,
    phone_verification_status VARCHAR(50),
    status VARCHAR(50) NOT NULL
);

-- Create job_profiles table
CREATE TABLE IF NOT EXISTS job_profiles (
    job_profile_uuid VARCHAR(255) PRIMARY KEY,
    organization_uuid VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    reporting_manager VARCHAR(255),
    organization_unit VARCHAR(255),
    extensions_data VARCHAR(1000)
);

-- Create user_reportees table
CREATE TABLE IF NOT EXISTS user_reportees (
    relation_uuid VARCHAR(255) PRIMARY KEY,
    organization_uuid VARCHAR(255) NOT NULL,
    manager_user_uuid VARCHAR(255) NOT NULL,
    user_uuid VARCHAR(255) NOT NULL,
    job_profile_uuid VARCHAR(255) NOT NULL
); 