# Multi-stage build for M10 Suscripciones y Pagos
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent POM and ALL module POMs first (for reactor resolution)
COPY pom.xml .
COPY gateway/pom.xml ./gateway/
COPY m1-identidad/pom.xml ./m1-identidad/
COPY m2-cuentas-fondos/pom.xml ./m2-cuentas-fondos/
COPY m3-catalogo-mercados/pom.xml ./m3-catalogo-mercados/
COPY m4-datos-mercado/pom.xml ./m4-datos-mercado/
COPY m5-ordenes-ejecucion/pom.xml ./m5-ordenes-ejecucion/
COPY m6-portafolio/pom.xml ./m6-portafolio/
COPY m8-notificaciones/pom.xml ./m8-notificaciones/
COPY m10-suscripciones-pagos/pom.xml ./m10-suscripciones-pagos/

# Copy the specific service source and build
COPY m10-suscripciones-pagos/src ./m10-suscripciones-pagos/src
RUN mvn -B -q -pl m10-suscripciones-pagos -am package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S -g 1001 appgroup \
    && adduser -S -u 1001 -G appgroup appuser
COPY --from=build --chown=appuser:appgroup /app/m10-suscripciones-pagos/target/m10-suscripciones-pagos-0.0.1-SNAPSHOT.jar /app/app.jar
USER appuser
EXPOSE 8090
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
