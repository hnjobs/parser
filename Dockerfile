FROM eclipse-temurin:21-jdk AS build

WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode --update-snapshots verify

FROM gcr.io/distroless/java21-debian12:nonroot

COPY --from=build /build/target/parser-1.0-SNAPSHOT.jar /app/parser.jar

ENTRYPOINT ["java", "-jar", "/app/parser.jar"]
