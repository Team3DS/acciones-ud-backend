# Multi-stage build for SERVICE_NAME
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent POM and ALL module POMs first (for reactor resolution)
COPY pom.xml .
COPY acciones-common/pom.xml ./acciones-common/
COPY gateway/pom.xml ./gateway/
COPY m1-identidad/pom.xml ./m1-identidad/
COPY m2-cuentas-fondos/pom.xml ./m2-cuentas-fondos/
COPY m3-catalogo-mercados/pom.xml ./m3-catalogo-mercados/
COPY m4-datos-mercado/pom.xml ./m4-datos-mercado/
COPY m5-ordenes-ejecucion/pom.xml ./m5-ordenes-ejecucion/
COPY m6-portafolio/pom.xml ./m6-portafolio/
COPY m8-notificaciones/pom.xml ./m8-notificaciones/
COPY m10-suscripciones-pagos/pom.xml ./m10-suscripciones-pagos/

# Copy common module source and install
COPY acciones-common/src ./acciones-common/src
RUN mvn -q -pl acciones-common -am install -DskipTests

# Copy the specific service source and build
COPY m5-ordenes-ejecucion/src ./m5-ordenes-ejecucion/src
RUN mvn -q -pl m5-ordenes-ejecucion -am package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/m5-ordenes-ejecucion/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","/app/app.jar"]
