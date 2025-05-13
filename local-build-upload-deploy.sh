#!/bin/bash

mvn clean package -DskipTests
scp -o StrictHostKeyChecking=no -i ~/user-api.pem ~/algobrewery/User-Management-Service/target/user-api-0.0.1-SNAPSHOT.jar ec2-user@ec2-3-88-101-241.compute-1.amazonaws.com:/home/ec2-user/user-api/app.jar