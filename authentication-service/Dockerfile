FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --uid 1001 appuser

COPY target/*.jar app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
