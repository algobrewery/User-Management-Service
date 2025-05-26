-- Insert test manager
INSERT INTO user_profiles (user_uuid, organization_uuid, username, first_name, last_name, middle_name, email, email_verification_status, phone, phone_country_code, phone_verification_status, start_date, end_date, status, job_profile_uuids)
VALUES ('manager-uuid-1', 'org-1', 'test-manager', 'Manager', 'Test', NULL, 'manager@example.com', 'VERIFIED', '1234567890', 1, 'VERIFIED', CURRENT_TIMESTAMP, NULL, 'Active', '{job-profile-manager-1}');

-- Insert test job profile for manager
INSERT INTO job_profiles (job_profile_uuid, organization_uuid, title, start_date, end_date, reporting_manager, organization_unit, extensions_data)
VALUES ('job-profile-manager-1', 'org-1', 'Manager', CURRENT_TIMESTAMP, NULL, '', 'Management', '{}');

-- Insert test user
INSERT INTO user_profiles (user_uuid, organization_uuid, username, first_name, last_name, middle_name, email, email_verification_status, phone, phone_country_code, phone_verification_status, start_date, end_date, status, job_profile_uuids)
VALUES ('user-uuid-1', 'org-1', 'test-user', 'User', 'Test', NULL, 'user@example.com', 'VERIFIED', '0987654321', 1, 'VERIFIED', CURRENT_TIMESTAMP, NULL, 'Active', '{job-profile-user-1}');

-- Insert test job profile for user
INSERT INTO job_profiles (job_profile_uuid, organization_uuid, title, start_date, end_date, reporting_manager, organization_unit, extensions_data)
VALUES ('job-profile-user-1', 'org-1', 'Developer', CURRENT_TIMESTAMP, NULL, 'manager-uuid-1', 'Engineering', '{}');

-- Insert user reportee relationship
INSERT INTO user_reportees (relation_uuid, organization_uuid, manager_user_uuid, user_uuid, job_profile_uuid)
VALUES ('relation-uuid-1', 'org-1', 'manager-uuid-1', 'user-uuid-1', 'job-profile-user-1');
