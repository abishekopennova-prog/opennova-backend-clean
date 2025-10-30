# Use Maven image to build the application
FROM maven:3.9-openjdk-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Use OpenJDK runtime image
FROM openjdk:21-jre-slim

# Set working directory
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/opennova-0.0.1-SNAPSHOT.jar app.jar

# Create uploads directory
RUN mkdir -p uploads/menu-images uploads/profile-images uploads/payment-screenshots

# Expose port
EXPOSE 10000

# Run the application
CMD ["java", "-jar", "app.jar"]