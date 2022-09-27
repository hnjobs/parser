FROM amazoncorretto:8

ADD target/lib /app/lib
ADD target/parser-1.0-SNAPSHOT.jar /app/parser.jar

WORKDIR /app

CMD ["java", "-jar", "parser.jar"]