FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/*.jar /app/

ENTRYPOINT ["java", "-jar", "rss.jar"]

# docker build -t akpeisov/rss:prod .
# docker push akpeisov/rss:prod 