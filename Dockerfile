FROM openjdk:17

WORKDIR /app

COPY . .

EXPOSE 8081

CMD ["java", "-jar", "target/Bootstrapper-1.0.jar"]