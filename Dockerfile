# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---- run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/currency-exchange-1.0.0.jar app.jar
ENV JAVA_OPTS="-Xmx384m"
EXPOSE 8080
# Render injects $PORT; application.properties reads it
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]