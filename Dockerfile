# Этап 1: Сборка (Build)
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Копируем зависимости
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходный код и собираем приложение
COPY src ./src
# Проверяем имя файла в pom.xml: <artifactId>Culture</artifactId> <version>0.0.1-SNAPSHOT</version>
RUN mvn clean package -DskipTests

# Этап 2: Запуск (Run)
# ИСПРАВЛЕНИЕ: Используем eclipse-temurin вместо openjdk
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Копируем JAR файл. Важно: имя файла должно совпадать с тем, что в pom.xml
COPY --from=build /app/target/Culture-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]