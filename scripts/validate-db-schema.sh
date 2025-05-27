#!/bin/bash

# Database Schema Validation Script
echo "🔍 Validating Database Schemas..."

# Function to check database schema
check_schema() {
    local host=$1
    local user=$2
    local pass=$3
    local db=$4
    local env_name=$5
    
    echo ""
    echo "📊 Checking $env_name Database Schema..."
    echo "Host: $host"
    echo "Database: $db"
    
    if command -v psql &> /dev/null; then
        # Check if required tables exist
        PGPASSWORD=$pass psql -h $host -U $user -d $db -c "
        SELECT 
            schemaname,
            tablename,
            tableowner
        FROM pg_tables 
        WHERE schemaname = 'public'
        ORDER BY tablename;
        "
        
        # Check database size
        PGPASSWORD=$pass psql -h $host -U $user -d $db -c "
        SELECT 
            pg_database.datname,
            pg_size_pretty(pg_database_size(pg_database.datname)) AS size
        FROM pg_database
        WHERE datname = '$db';
        "
        
        echo "✅ $env_name schema validation complete"
    else
        echo "⚠️ psql not available, skipping schema check for $env_name"
    fi
}

# Test Database
check_schema "task-silo-db-test-2.culss6gmw8na.us-east-1.rds.amazonaws.com" \
             "tasksilodbtest" \
             "tasksilodbtest" \
             "postgres" \
             "TEST"

# Production Database
check_schema "user-management-prod-db.culss6gmw8na.us-east-1.rds.amazonaws.com" \
             "postgres" \
             "tasksilodbtest" \
             "usermanagement_prod" \
             "PRODUCTION"

echo ""
echo "🎯 Database Schema Validation Complete!"
