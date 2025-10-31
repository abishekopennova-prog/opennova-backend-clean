# Use Eclipse Temurin Maven image to build the application
FROM eclipse-temurin:17-jdk AS build

# Install Maven
RUN apt-get update && apt-get install -y maven

# Set working directory
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Use Eclipse Temurin runtime image
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/opennova-backend-1.0.0.jar app.jar

# Create uploads directory
RUN mkdir -p uploads/menu-images uploads/profile-images uploads/payment-screenshots

# Expose port
EXPOSE 9000

# Run the application
CMD ["java", "-jar", "app.jar"]