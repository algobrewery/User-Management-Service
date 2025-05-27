-- Production Database Schema Setup
-- Run this script to create required tables in production database

-- Create job_profiles table (the missing table causing the error)
CREATE TABLE IF NOT EXISTS job_profiles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    department VARCHAR(255),
    level VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create other tables that might be needed
-- (Add more CREATE TABLE statements here as needed)

-- Insert some default data if needed
INSERT INTO job_profiles (title, description, department, level) 
VALUES 
    ('Software Engineer', 'Develops and maintains software applications', 'Engineering', 'Mid'),
    ('Senior Software Engineer', 'Senior level software development role', 'Engineering', 'Senior'),
    ('Product Manager', 'Manages product development lifecycle', 'Product', 'Mid')
ON CONFLICT DO NOTHING;

-- Grant permissions (adjust as needed)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO your_app_user;
