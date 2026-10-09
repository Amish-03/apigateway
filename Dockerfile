# Runtime-only image. The jar is built by the CI pipeline (mvnw package)
# before `docker build` runs, so Maven is never invoked here.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 6005
ENTRYPOINT ["java", "-jar", "app.jar"]
