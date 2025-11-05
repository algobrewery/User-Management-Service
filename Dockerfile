# Use a lightweight OpenJDK image
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copy the JAR file
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
