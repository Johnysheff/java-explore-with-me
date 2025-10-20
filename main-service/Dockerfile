FROM maven:3.9.6-eclipse-temurin-21 as builder

WORKDIR /workspace

# копируем структуру проекта
COPY . .

# устанавливаем корневой pom и все модули в локальный репозиторий
RUN mvn install -N -DskipTests && \
    mvn -f stats/pom.xml install -N -DskipTests && \
    mvn -f stats/stats-dto/pom.xml install -DskipTests && \
    mvn -f stats/stats-client/pom.xml install -DskipTests

# cборка main-service
RUN mvn -f main-service/pom.xml clean package -DskipTests
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /workspace/main-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]