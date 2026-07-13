FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/genexplus-*.jar /app/genexplus.jar
COPY target/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*:/app/genexplus.jar", "io.github.stefanbln.genexplus.report.Main"]
