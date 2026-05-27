FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/monolith-0.1.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
