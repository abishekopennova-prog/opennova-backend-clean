# Use OpenJDK 21 as base image
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn ./.mvn

# Copy source code
COPY src ./src

# Make mvnw executable
RUN chmod +x ./mvnw

# Build the application
RUN ./mvnw clean package -DskipTests

# Create uploads directory
RUN mkdir -p uploads/menu-images uploads/profile-images uploads/payment-screenshots

# Expose port
EXPOSE 10000

# Run the application
CMD ["java", "-jar", "target/opennova-0.0.1-SNAPSHOT.jar"]