# Use a base image with Java 17 already installed
FROM eclipse-temurin:17-jdk-alpine
# Expose the port your app runs on
EXPOSE 8082
# Set working directory inside the container
WORKDIR /tmp

# Copy the built JAR file into the container
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app.jar

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app.jar"]



