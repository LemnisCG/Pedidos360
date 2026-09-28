# Etapa 1: compilar la aplicación con Maven y Java 21
FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

# Copiamos primero el archivo Maven
COPY pom.xml .

# Copiamos el código fuente
COPY src ./src

# Compilamos sin ejecutar tests
RUN mvn -q -DskipTests clean package


# Etapa 2: imagen liviana para ejecutar el microservicio
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiamos el JAR generado
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]