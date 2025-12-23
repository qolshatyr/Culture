# Этап 1: Сборка (Build)
# Используем образ с Maven и Java 17
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Копируем файл зависимостей и скачиваем их (для кэширования)
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходный код и собираем приложение
COPY src ./src
# Собираем JAR, пропуская тесты для скорости
RUN mvn clean package -DskipTests

# Этап 2: Запуск (Run)
# Используем легкий образ Java 17 для запуска
FROM openjdk:17-jdk-slim
WORKDIR /app

# Копируем собранный JAR файл из этапа сборки
# Имя файла берется из pom.xml: <artifactId>Culture</artifactId> + <version>0.0.1-SNAPSHOT</version>
COPY --from=build /app/target/Culture-0.0.1-SNAPSHOT.jar app.jar

# Открываем порт 8080
EXPOSE 8080

# Команда запуска
ENTRYPOINT ["java", "-jar", "app.jar"]