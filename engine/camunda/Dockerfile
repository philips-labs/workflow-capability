# Use the Maven image as the builder stage
FROM maven:3.8.6-openjdk-11 as builder

WORKDIR /app
COPY . /app

# Build the application using Maven
RUN mvn -B package --file pom.xml

# Create a new stage with the OpenJDK 11 JRE slim image
FROM openjdk:11-jre-slim

# Set the working directory for the final image
WORKDIR /app

COPY --from=builder /app/target/camunda_engine-0.0.2-SNAPSHOT.jar /app
CMD ["java", "-jar", "camunda_engine-0.0.2-SNAPSHOT.jar"]