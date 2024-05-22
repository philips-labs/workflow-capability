# Use an official Maven image to build the project
FROM maven:3.8.6-openjdk-11 as builder

# Set the working directory in the container
WORKDIR /app

# Copy the Java project files into the container
COPY . /app

# Build the project using Maven
RUN mvn -B package --file pom.xml

# Use an official OpenJDK image to run the jar
FROM openjdk:11-jre-slim

# Set the working directory in the container
WORKDIR /app

# Copy only the built jar file from the builder stage
COPY --from=builder /app/target/camunda_engine-0.0.2-SNAPSHOT.jar /app

# Command to run the jar file
CMD ["java", "-jar", "camunda_engine-0.0.2-SNAPSHOT.jar"]
