# Use a lightweight OpenJDK image
FROM openjdk:17-slim
WORKDIR /app
# Copy the JAR file
COPY build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
