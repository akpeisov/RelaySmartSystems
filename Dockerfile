FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/*.jar /app/

ENTRYPOINT ["java", "-jar", "rss.jar"]