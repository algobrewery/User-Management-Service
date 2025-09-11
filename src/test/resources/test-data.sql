-- Clear existing data (in reverse order of dependencies)
DELETE FROM user_reportees;
DELETE FROM job_profiles;
DELETE FROM user_profiles;

-- Insert job profiles first (no dependencies)
INSERT INTO job_profiles (job_profile_uuid, organization_uuid, title, start_date, reporting_manager, organization_unit, extensions_data)
VALUES
('job-profile-2', 'org-1', 'Manager', CURRENT_TIMESTAMP, '', 'Management', '{}'),
('job-profile-1', 'org-1', 'Software Engineer', CURRENT_TIMESTAMP, 'test-user-2', 'Engineering', '{}'),
('job-profile-3', 'org-1', 'Designer', CURRENT_TIMESTAMP, 'test-user-2', 'Design', '{}');

-- Insert test manager (user 2) - depends on job-profile-2
INSERT INTO user_profiles (user_uuid, organization_uuid, username, first_name, last_name, start_date, job_profile_uuids, email, email_verification_status, phone, phone_country_code, phone_verification_status, status)
VALUES
('test-user-2', 'org-1', 'manager-1', 'Jane', 'Smith', CURRENT_TIMESTAMP, '{job-profile-2}', 'jane.smith@example.com', 'VERIFIED', '0987654321', 1, 'VERIFIED', 'Active');

-- Insert test users - depends on job-profile-1 and job-profile-3
INSERT INTO user_profiles (user_uuid, organization_uuid, username, first_name, last_name, start_date, job_profile_uuids, email, email_verification_status, phone, phone_country_code, phone_verification_status, status)
VALUES
('test-user-1', 'org-1', 'john.doe', 'John', 'Doe', CURRENT_TIMESTAMP, '{job-profile-1}', 'john.doe@example.com', 'VERIFIED', '1234567890', 1, 'VERIFIED', 'Active'),
('test-user-3', 'org-1', 'user3', 'Bob', 'Johnson', CURRENT_TIMESTAMP, '{job-profile-3}', 'bob.johnson@example.com', 'VERIFIED', '5555555555', 1, 'VERIFIED', 'Active');

-- Insert reporting relationships last - depends on both user_profiles and job_profiles
INSERT INTO user_reportees (relation_uuid, organization_uuid, manager_user_uuid, user_uuid, job_profile_uuid)
VALUES
('rel-1', 'org-1', 'test-user-2', 'test-user-1', 'job-profile-1'),
('rel-2', 'org-1', 'test-user-2', 'test-user-3', 'job-profile-3');