# eterna-preingest

Generisk pre-ingest-plattform för [ETERNA](https://github.com/ETERNA-earkiv) digitalt bevarandesystem.

## Moduler

| Modul | Beskrivning |
|---|---|
| [`eterna-sip-commons`](eterna-sip-commons/) | Delat Java-bibliotek: ETERNA REST-klient, SIP-paketering, ingest-orkestrering |
| [`eterna-ingest-service`](eterna-ingest-service/) | Generisk Spring Boot-tjänst med konfigurerbart schema och REST API |
| [`eterna-ingest-ui`](eterna-ingest-ui/) | React-webgränssnitt för manuell inleverans |

Befintliga repos som berörs ej:
- [`Preingest01/`](Preingest01/) — Batch-ETL från OLDIA/AF-DIA
- [`Preingest02/`](Preingest02/) — Tea-Connect (case/item API)

---

## Quick Start

### 1. Förbered schema

Välj eller skapa ett schema baserat på exemplen:

```bash
cp examples/schema-arende.yaml /din/config/schema.yaml
# Redigera fält efter behov
```

### 2. Starta tjänsten

```bash
export ETERNA_URL=http://din-eterna:8080
export ETERNA_USERNAME=admin
export ETERNA_PASSWORD=eterna
export ETERNA_PARENT_ID=<uuid-till-mål-aip-i-eterna>
export GITHUB_USERNAME=<ditt-github-användarnamn>
export GITHUB_TOKEN=<github-token-med-read:packages>

docker compose up -d
```

Tjänsten startar på **http://localhost:8082**.

### 3. Installera ETERNA config overlay

Vid start genereras ETERNA-konfigurationsfiler automatiskt:

```bash
# Kopiera overlay-filer till ETERNA:s config-katalog
docker compose cp eterna-ingest-service:/config/overlay/. /din/eterna/config/
# Starta om ETERNA för att ladda ny metadata-typ
```

### 4. Testa

**Via webgränssnitt:** Öppna http://localhost:8082

**Via API:**
```bash
curl -X POST http://localhost:8082/api/records \
  -H "Content-Type: application/json" \
  -d '{
    "parentId": "<uuid>",
    "recordType": "RECORD",
    "fields": {
      "arendenummer": "2024-001",
      "titel": "Testärende",
      "klassifikationskod": "3.1",
      "oppningsdatum": "2024-01-15",
      "arkiveringsdatum": "2024-12-31"
    }
  }'

# Polla status
curl http://localhost:8082/api/records/<jobId>
```

**Swagger UI:** http://localhost:8082/swagger-ui.html

---

## Konfiguration

### Miljövariabler

| Variabel | Default | Beskrivning |
|---|---|---|
| `ETERNA_URL` | `http://localhost:8080` | ETERNA-instansens URL |
| `ETERNA_USERNAME` | `admin` | Användarnamn |
| `ETERNA_PASSWORD` | `eterna` | Lösenord |
| `ETERNA_API_PATH` | `/api/v1` | API-prefix (varierar per ETERNA-version) |
| `ETERNA_PARENT_ID` | — | Standard mål-AIP för inleveranser |
| `INGEST_SCHEMA_PATH` | `/config/schema.yaml` | Sökväg till schema-fil |
| `INGEST_OVERLAY_DIR` | `/config/overlay` | Katalog för genererade ETERNA config-filer |
| `INGEST_AUTO_DEPLOY_OVERLAY` | `true` | Generera overlay vid startup |

### schema.yaml-format

```yaml
record:
  metadataType: "min-typ"      # Unikt namn som ETERNA använder internt
  label:
    sv: "Ärende"
    en: "Case"

item:
  metadataType: "min-handling"
  label:
    sv: "Handling"
    en: "Document"

recordFields:
  - name: fältnamn             # Används som XML-elementnamn
    type: string               # string|text|date|datetime|integer|decimal|bool|email|url|enumeration
    required: true
    label: { sv: "Visningsnamn", en: "Display Name" }

  - name: status
    type: enumeration
    values: [Aktiv, Avslutad]  # Tillåtna värden för enum-typ
    required: false

itemFields:
  # Samma struktur som recordFields
```

---

## Arkitektur

```
schema.yaml (kund-konfiguration)
      │
      ▼
eterna-ingest-service (Spring Boot :8080)
  ├── POST /api/records          ← Teknisk integration
  ├── GET  /api/records/{jobId}  ← Statuspollning
  ├── GET  /api/schema           ← JSON Schema för UI
  └── GET  / (UI)                ← Webbgränssnitt för slutanvändare
      │
      ▼
eterna-sip-commons
  ├── SchemaValidator    (validerar mot schema.yaml)
  ├── MetadataXmlGenerator (skapar XML från fältvärden)
  ├── SipPackager        (E-ARK SIP via commons-ip2)
  └── EternaClient       (REST-kommunikation med ETERNA)
      │
      ▼
ETERNA (digital preservation repository)
```

---

## Bygga lokalt

**Förutsättningar:** Java 21, Maven 3.9+, Node 20+, GitHub-token med `read:packages`

```bash
# Maven-modulerna
mvn clean package -DskipTests

# React UI (byggs automatiskt in i JAR vid Maven-bygget)
cd eterna-ingest-ui && npm ci && npm run build

# Kör tjänsten
cd eterna-ingest-service
INGEST_SCHEMA_PATH=../examples/schema-arende.yaml \
ETERNA_URL=http://localhost:8080 \
java -jar target/eterna-ingest-service-*.jar
```

---

## Exempelscheman

| Fil | Användningsfall |
|---|---|
| [`examples/schema-arende.yaml`](examples/schema-arende.yaml) | Ärendehantering för kommuner/myndigheter |
| [`examples/schema-dokument.yaml`](examples/schema-dokument.yaml) | Dokumentsamlingar och projektarkiv |
