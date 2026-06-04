# eterna-preingest

Generisk pre-ingest-plattform för [ETERNA](https://github.com/ETERNA-earkiv) digitalt bevarandesystem.

Kunder konfigurerar ett schema i YAML som beskriver sina metadata-fält — plattformen sköter validering, E-ARK SIP-paketering och ingest mot ETERNA.

---

## Moduler

| Modul | Beskrivning | Dokumentation |
|---|---|---|
| `eterna-sip-commons` | Delat Java-bibliotek: ETERNA-klient, SIP-paketering, ingest-orkestrering | [README](eterna-sip-commons/README.md) |
| `eterna-ingest-service` | Spring Boot REST API med konfigurerbart schema och overlay-generering | [README](eterna-ingest-service/README.md) |
| `eterna-ingest-ui` | React-webgränssnitt för manuell inleverans | [README](eterna-ingest-ui/README.md) |

---

## Snabbstart

```bash
export ETERNA_URL=http://din-eterna:8080
export ETERNA_USERNAME=admin
export ETERNA_PASSWORD=eterna
export ETERNA_PARENT_ID=<uuid-till-mål-aip>
export GITHUB_USERNAME=<ditt-github-användarnamn>
export GITHUB_TOKEN=<github-token-med-read:packages>

docker compose up -d
```

Tjänsten startar på **http://localhost:8082**.

Se [eterna-ingest-service/README.md](eterna-ingest-service/README.md) för fullständig konfiguration och API-referens.

---

## Använda eterna-sip-commons som bibliotek

```xml
<dependency>
    <groupId>se.eterna</groupId>
    <artifactId>eterna-sip-commons</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Spring Boot auto-konfigurerar `EternaClient`, `IngestService`, `BatchIngestService`, `SipPackager` och `RecordSipBuilder` automatiskt när `eterna.url` är satt.

Se [eterna-sip-commons/README.md](eterna-sip-commons/README.md) för fullständig biblioteksreferens.

---

## Arkitektur

```
schema.yaml (kundkonfiguration)
      │
      ▼
eterna-ingest-service :8082
  ├── POST /api/records    ← Ta emot metadata + filer
  ├── GET  /api/records/{jobId} ← Polla ingest-status
  ├── GET  /api/schema     ← JSON Schema för UI
  └── OverlayGenerator     ← Genererar ETERNA config vid startup
      │
      ▼
eterna-sip-commons
  ├── exception/   EternaClientException, IngestTimeoutException, ...
  ├── client/      EternaClient, ApiResult<T>
  ├── ingest/      IngestService (retry), BatchIngestService
  ├── sip/         SipPackager, SipOutputTarget
  │   └── domain/  ArchivableRecord, RecordSipBuilder
  ├── io/          DataBufferOutputStream, StreamProcessor
  └── xml/         StaxXmlValidator
      │
      ▼
ETERNA
  ├── Transfer API  ← Mottar SIP ZIP
  ├── Jobs API      ← Kör ConfigurableIngestPlugin
  └── AIP           ← Resultatet: arkiverat informationspaket
```

---

## Bygga lokalt

**Förutsättningar:** Java 21, Maven 3.9+, Node 20+, GitHub-token med `read:packages`

```bash
mvn clean package -DskipTests
```
