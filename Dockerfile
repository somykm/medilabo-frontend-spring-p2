# Use a base image with Java 17 already installed
FROM openjdk:17-jdk-slim

# Set working directory inside the container
WORKDIR /app

# Copy the built JAR file into the container
COPY target/medilabo-frontend-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your app runs on
EXPOSE 8082

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]