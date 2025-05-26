-- Cleanup script for test data
-- This script removes test data to ensure clean state between tests

-- Delete test data in correct order to respect foreign key constraints
DELETE FROM user_reportees WHERE organization_uuid = 'org-1';
DELETE FROM job_profiles WHERE organization_uuid = 'org-1';
DELETE FROM user_profiles WHERE organization_uuid = 'org-1';

-- Reset sequences if needed (PostgreSQL specific)
-- Note: Only reset if using sequences for auto-generated IDs
