FROM gcr.io/distroless/java21-debian12:nonroot

COPY target/parser-1.0-SNAPSHOT.jar /app/parser.jar

ENTRYPOINT ["java", "-jar", "/app/parser.jar"]
