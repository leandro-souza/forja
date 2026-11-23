# 1) Interface web (React + Vite)
FROM node:22-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2) API (Spring Boot), com a interface embutida como conteudo estatico
FROM maven:3.9-eclipse-temurin-21 AS api
WORKDIR /app
COPY backend/pom.xml .
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -B package

# 3) Imagem final, so com o JRE
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tzdata
ENV TZ=America/Bahia
WORKDIR /app
COPY --from=api /app/target/forja.jar forja.jar
EXPOSE 8080
# Monta a URL JDBC a partir de DB_HOST/DB_PORT/DB_NAME quando o banco e fornecido pela plataforma
ENTRYPOINT ["sh", "-c", "if [ -n \"$DB_HOST\" ]; then export JDBC_DATABASE_URL=\"jdbc:postgresql://$DB_HOST:${DB_PORT:-5432}/$DB_NAME\"; fi; exec java -XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Duser.timezone=America/Bahia -jar forja.jar"]
