# syntax=docker/dockerfile:1

# 1단계: gradle wrapper로 jar 빌드
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# 의존성 캐시 활용을 위해 wrapper와 빌드 설정 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --version

# 소스 복사 후 빌드. 테스트는 Docker(Testcontainers)를 요구하므로 이미지 빌드에서 뺀다.
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# 2단계: 런타임 이미지
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENV PORT=8081
EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]
