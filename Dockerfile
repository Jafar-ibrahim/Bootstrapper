FROM openjdk:17

WORKDIR /app

COPY . .

EXPOSE 8080

CMD ["java", "-jar", "target/Bootstrapper.jar"]