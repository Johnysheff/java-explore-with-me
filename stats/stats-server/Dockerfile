FROM maven:3.9.6-eclipse-temurin-21 as builder

WORKDIR /workspace

# копируем структуру проекта.
COPY . .

# устанавливаем корневой pom и все модули в локальный репозиторий
RUN mvn install -N -DskipTests && \
    mvn -f stats/pom.xml install -N -DskipTests && \
    mvn -f stats/stats-dto/pom.xml install -DskipTests

# сборка stats-server
RUN mvn -f stats/stats-server/pom.xml clean package -DskipTests
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /workspace/stats/stats-server/target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]