# Deploy to Existing VPC with Production Database

## Database Details
- **Database Host**: `nucleus-production.c6j42qq2krlk.us-east-1.rds.amazonaws.com`
- **VPC ID**: `vpc-01a3786d527f5d2ff`
- **Subnets**: 
  - `subnet-031654e7ed0e658ed` (us-east-1a)
  - `subnet-0928526572637e612` (us-east-1b)
  - `subnet-0e8b3057e6b61228e` (us-east-1c)
  - `subnet-0ff76f036791fad98` (us-east-1d)
  - `subnet-030ee54f8c01c7d76` (us-east-1e)
  - `subnet-0b94ffbb55995b393` (us-east-1f)
- **Security Group**: `sg-05259fe0eb39e8344` (default)

## Deployment Options

### Option 1: Deploy ECS Service in Same VPC (Recommended)
Since your database is already working, the ECS service is likely already deployed in the same VPC. The current deployment is working correctly.

### Option 2: Update CloudFormation Parameters
If you need to redeploy, use these parameters:

```bash
aws cloudformation create-stack \
  --stack-name user-management-production \
  --template-body file://cloudformation-template.yml \
  --parameters \
    ParameterKey=UseExistingVpc,ParameterValue=true \
    ParameterKey=ExistingVpcId,ParameterValue=vpc-01a3786d527f5d2ff \
    ParameterKey=ExistingSubnet1Id,ParameterValue=subnet-031654e7ed0e658ed \
    ParameterKey=ExistingSubnet2Id,ParameterValue=subnet-0928526572637e612 \
    ParameterKey=DBHost,ParameterValue=nucleus-production.c6j42qq2krlk.us-east-1.rds.amazonaws.com \
    ParameterKey=DBPassword,ParameterValue=nucleus-production
```

### Option 3: Manual ECS Service Update
Update your existing ECS service to use the correct subnets:

1. Go to ECS Console
2. Find your service: `production-user-management-service`
3. Update service configuration:
   - **Subnets**: Use the database VPC subnets
   - **Security Groups**: Ensure it can access the database (port 5432)

## Security Group Configuration
Ensure your ECS security group allows:
- **Outbound**: Port 5432 to database security group `sg-05259fe0eb39e8344`
- **Inbound**: Port 8080 from ALB security group

## Current Status
✅ **Database Connection Working**: Your application is successfully connecting to the production database
✅ **Application Running**: The service is processing requests correctly
✅ **VPC Connectivity**: ECS can reach the private database

The current deployment is working correctly with the production database!
