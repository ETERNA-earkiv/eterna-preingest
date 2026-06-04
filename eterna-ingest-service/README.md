# eterna-ingest-service

Generisk Spring Boot-tjänst som tar emot metadata och filer via REST API, paketerar dem som E-ARK SIP och ingesta till ETERNA. Tjänsten konfigureras med ett schema i YAML och genererar automatiskt de ETERNA config-filer (overlay) som behövs för custom metadata-typer.

---

## Snabbstart

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

---

## Miljövariabler

| Variabel | Default | Beskrivning |
|---|---|---|
| `ETERNA_URL` | `http://localhost:8080` | ETERNA-instansens URL |
| `ETERNA_USERNAME` | `admin` | Användarnamn |
| `ETERNA_PASSWORD` | `eterna` | Lösenord |
| `ETERNA_API_PATH` | `/api/v2` | API-prefix |
| `ETERNA_PARENT_ID` | — | Standard mål-AIP för inleveranser |
| `INGEST_SCHEMA_PATH` | `/config/schema.yaml` | Sökväg till schema-fil |
| `INGEST_OVERLAY_DIR` | `/config/overlay` | Katalog för genererade ETERNA config-filer |
| `INGEST_AUTO_DEPLOY_OVERLAY` | `true` | Generera overlay vid startup |
| `INGEST_WORK_DIR` | `/tmp/eterna-ingest` | Temporär katalog för SIP-byggning |

---

## schema.yaml

Schemat styr vilka metadata-fält som accepteras, valideras och paketeras i SIP:en.

```yaml
record:
  metadataType: "arende"
  label:
    sv: "Ärende"
    en: "Case"

item:
  metadataType: "handling"
  label:
    sv: "Handling"
    en: "Document"

recordFields:
  - name: arendenummer
    type: string
    required: true
    label:
      sv: "Ärendenummer"
      en: "Case number"

  - name: status
    type: enumeration
    required: false
    values: ["Aktiv", "Avslutad"]

itemFields:
  - name: titel
    type: string
    required: true
    label:
      sv: "Titel"
      en: "Title"
```

### Fälttyper

| Typ | Validering | Solr-suffix |
|---|---|---|
| `string` | — | `_txt` |
| `text` | — | `_txt` |
| `date` | YYYY-MM-DD | `_dt` |
| `datetime` | ISO 8601 | `_dt` |
| `integer` | Numerisk | `_txt` |
| `decimal` | Numerisk | `_txt` |
| `bool` | `true`/`false` | `_txt` |
| `email` | — | `_txt` |
| `url` | — | `_txt` |
| `enumeration` | Måste finnas i `values` | `_txt` |

### Exempelscheman

| Fil | Metadata-typ | Användningsfall |
|---|---|---|
| [`examples/schema-arende.yaml`](../examples/schema-arende.yaml) | `arende` + `handling` | Ärendehantering |
| [`examples/schema-ead-2002.yaml`](../examples/schema-ead-2002.yaml) | `ead_2002` | EAD 2002 / ISAD(G) |
| [`examples/schema-ead-3.yaml`](../examples/schema-ead-3.yaml) | `ead_3` | EAD 3 |

---

## REST API

Bas-URL: `http://localhost:8082`

Fullständig specifikation: **http://localhost:8082/swagger-ui.html**

### POST `/api/records` — Skicka post för arkivering

```json
{
  "parentId": "550e8400-e29b-41d4-a716-446655440000",
  "recordType": "RECORD",
  "fields": {
    "arendenummer": "2024-042",
    "titel": "Bygglov Storgatan 5",
    "oppningsdatum": "2024-03-01"
  },
  "files": [
    { "filename": "ansökan.pdf", "base64Data": "JVBERi0xLjQK..." }
  ]
}
```

| Fält | Typ | Krav |
|---|---|---|
| `parentId` | string (UUID) | Ja |
| `recordType` | `RECORD` \| `ITEM` | Ja |
| `fields` | object | Ja |
| `files` | array | Nej |

**Svar 202:**

```json
{ "jobId": "abc-def-123" }
```

**Svar 400 (valideringsfel):**

```json
{
  "error": "Valideringsfel",
  "details": ["arende: obligatoriskt fält saknas: arendenummer"]
}
```

### GET `/api/records/{jobId}` — Hämta ingest-status

```json
{
  "jobId": "abc-def-123",
  "status": "COMPLETED",
  "percentageCompleted": 100,
  "aipId": null
}
```

| Statusvärde | Beskrivning |
|---|---|
| `CREATED` | Väntar på start |
| `STARTED` | Pågår |
| `COMPLETED` | Lyckades |
| `COMPLETED_WITH_PROBLEMS` | Lyckades med varningar |
| `FAILED_TO_COMPLETE` | Misslyckades under körning |
| `FAILED_DURING_CREATION` | Misslyckades när jobbet skapades/startades |
| `FAILED_DURING_INGEST` | Misslyckades under ingest (bakåtkompatibilitet) |
| `FAILED` | Misslyckades |
| `STOPPED` | Avbrutet |
| `PENDING_APPROVAL` | Väntar på godkännande |
| `REJECTED` | Avvisat |
| `SCHEDULED` | Schemalagt |

### GET `/api/schema` — Hämta JSON Schema

Returnerar schemat i JSON Schema Draft-07 för UI-rendering.

---

## ETERNA overlay-systemet

Vid startup genereras ETERNA config-filer baserat på schemat:

```
overlay/
├── schemas/         arende.xsd, handling.xsd
├── crosswalks/
│   ├── ingest/      arende.xslt, handling.xslt  (XML → Solr-fält)
│   └── dissemination/html/  (XML → HTML i ETERNA UI)
├── templates/       arende.xml.hbs, handling.xml.hbs
└── i18n/            ServerMessages.properties, ServerMessages_sv_SE.properties
```

### Installera overlay i ETERNA

```bash
docker compose cp eterna-ingest-service:/config/overlay/. /path/to/eterna/config/
docker compose restart eterna
```

---

## Bygga lokalt

```bash
# Bygg hela projektet
mvn clean package -DskipTests

# Kör tjänsten
cd eterna-ingest-service
INGEST_SCHEMA_PATH=../examples/schema-arende.yaml \
ETERNA_URL=http://localhost:8080 \
java -jar target/eterna-ingest-service-*.jar
```
