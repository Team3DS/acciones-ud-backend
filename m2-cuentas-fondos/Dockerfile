FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

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

COPY m2-cuentas-fondos/src ./m2-cuentas-fondos/src
RUN mvn -B -q -pl m2-cuentas-fondos -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S -g 1001 appgroup \
    && adduser -S -u 1001 -G appgroup appuser
COPY --from=build /app/m2-cuentas-fondos/target/*.jar /app/app.jar
RUN chown appuser:appgroup /app/app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
