# Use Eclipse Temurin JDK 21 as base
FROM eclipse-temurin:21-jdk

# Install dependencies for JavaFX (GTK for Linux)
RUN apt-get update && apt-get install -y libgtk-3-0 libxext6 libxrender1 libxtst6 libxi6 && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first for dependency caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the source
COPY src ./src
COPY Images ./Images

# Build the application
RUN ./mvnw clean package -DskipTests

# Set the display environment variable for X11
ENV DISPLAY=:0

# Expose no ports (GUI only)

# Run the JavaFX application
CMD ["./mvnw", "javafx:run"] 