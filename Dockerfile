FROM openjdk:8

RUN mkdir /app

WORKDIR /app

ADD ./api/target/recomender-api-1.0.0-SNAPSHOT.jar /app

EXPOSE 8081

CMD ["java", "-jar", "recomender-api-1.0.0-SNAPSHOT.jar"]