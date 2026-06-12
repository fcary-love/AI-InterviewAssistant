FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY pom.xml .
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y tesseract-ocr tesseract-ocr-chi-sim && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/data /app/tessdata
EXPOSE 8002
ENTRYPOINT ["java", "-jar", "app.jar"]
