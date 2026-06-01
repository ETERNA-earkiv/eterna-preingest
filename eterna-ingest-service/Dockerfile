# ─── Steg 1: Bygg React UI ───────────────────────────────────────────────────
FROM node:20-alpine AS ui-builder
WORKDIR /build/eterna-ingest-ui
COPY eterna-ingest-ui/package.json eterna-ingest-ui/package-lock.json* ./
RUN npm ci
COPY eterna-ingest-ui/ .
RUN mkdir -p ../eterna-ingest-service/src/main/resources/static && npm run build

# ─── Steg 2: Bygg Java-tjänsten ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS java-builder
WORKDIR /build

# Kopiera in UI-bygget som statiska resurser
COPY --from=ui-builder /build/eterna-ingest-service/src/main/resources/static /build/eterna-ingest-service/src/main/resources/static

# Kopiera Maven-filer och ladda ner beroenden (cachat lager)
COPY pom.xml .
COPY eterna-sip-commons/pom.xml eterna-sip-commons/
COPY eterna-ingest-service/pom.xml eterna-ingest-service/

# Autentisering mot GitHub Packages — kräver GITHUB_USERNAME och GITHUB_TOKEN som build-args
ARG GITHUB_USERNAME
ARG GITHUB_TOKEN
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<EOF
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>${GITHUB_USERNAME}</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

RUN mvn dependency:go-offline -q

COPY eterna-sip-commons/src eterna-sip-commons/src
COPY eterna-ingest-service/src eterna-ingest-service/src

RUN mvn package -DskipTests -q

# ─── Steg 3: Runtime-image ───────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S eterna && adduser -S eterna -G eterna

COPY --from=java-builder /build/eterna-ingest-service/target/*.jar app.jar

RUN mkdir -p /config /config/overlay /tmp/eterna-ingest && \
    chown -R eterna:eterna /config /tmp/eterna-ingest

USER eterna

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
