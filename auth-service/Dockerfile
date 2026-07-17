# ============================================
# Stage 1: Build com Maven
# ============================================
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copiar TODOS os POMs (Maven precisa resolver o reactor completo)
COPY pom.xml .
COPY shared-kernel/pom.xml shared-kernel/
COPY auth-service/pom.xml auth-service/
COPY chat-service/pom.xml chat-service/

# Baixar dependencias (cached se POMs nao mudarem)
RUN mvn dependency:go-offline -pl auth-service -am -B

# Copiar codigo-fonte
COPY shared-kernel/src shared-kernel/src
COPY auth-service/src auth-service/src

# Compilar (sem testes — ja rodaram no CI)
RUN mvn package -pl auth-service -am -DskipTests -B

# ============================================
# Stage 2: Runtime minimo
# ============================================
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /app/auth-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
