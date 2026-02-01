# ---- build stage ----
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY docs ./docs

RUN mvn -DskipTests clean package && \
    mvn -q dependency:copy-dependencies -DincludeScope=runtime


FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/classes ./target/classes
COPY --from=build /app/target/dependency ./target/dependency
COPY --from=build /app/docs ./docs

ENV OPENAI_MODEL=gpt-4o-mini


CMD ["java", "-cp", "target/classes:target/dependency/*", "com.example.app.Main"]
