FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/m4-datos-mercado-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8084

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]