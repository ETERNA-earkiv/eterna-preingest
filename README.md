# eterna-preingest

Generisk pre-ingest-plattform för [ETERNA](https://github.com/ETERNA-earkiv) digitalt bevarandesystem.

Användare bygger ett anpassat inleveransflöde genom att definiera ett schema i YAML — tjänsten sköter validering, E-ARK SIP-paketering och automatisk ingest mot ETERNA.

---

## Moduler

| Modul | Beskrivning |
|---|---|
| [`eterna-sip-commons`](eterna-sip-commons/) | Delat Java-bibliotek: ETERNA REST-klient, SIP-paketering, ingest-orkestrering |
| [`eterna-ingest-service`](eterna-ingest-service/) | Generisk Spring Boot-tjänst med konfigurerbart schema och REST API |
| [`eterna-ingest-ui`](eterna-ingest-ui/) | React-webgränssnitt för manuell inleverans |

---

## Snabbstart

### 1. Välj eller skapa ett schema

Kopiera ett exempelschema och anpassa fälten efter er verksamhet:

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

Vid start genereras ETERNA-konfigurationsfiler automatiskt baserat på ert schema. Dessa måste kopieras till ETERNAs config-katalog och ETERNA måste startas om för att känna igen den nya metadata-typen:

```bash
# Kopiera overlay-filer till ETERNA:s config-katalog
docker compose cp eterna-ingest-service:/config/overlay/. /din/eterna/config/
# Starta om ETERNA för att ladda ny metadata-typ
```

### 4. Testa

**Via webgränssnitt:** Öppna http://localhost:8082

**Via API:**
```bash
# Skicka ett ärende med en bifogad fil (base64-kodad)
curl -X POST http://localhost:8082/api/records \
  -H "Content-Type: application/json" \
  -d '{
    "parentId": "<uuid-till-mål-aip>",
    "recordType": "RECORD",
    "fields": {
      "arendenummer": "2024-001",
      "titel": "Testärende",
      "klassifikationskod": "3.1",
      "oppningsdatum": "2024-01-15",
      "arkiveringsdatum": "2024-12-31"
    }
  }'
# Svar: { "jobId": "abc-123" }

# Polla status
curl http://localhost:8082/api/records/abc-123
# Svar: { "jobId": "abc-123", "status": "COMPLETED", "percentageCompleted": 100, "aipId": null }
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

### schema.yaml — komplett format

Schemat styr vilka metadata-fält som accepteras, valideras och paketeras i SIP:en.

```yaml
# record = behållaren (t.ex. ärende, samling, projekt)
record:
  metadataType: "min-typ"      # Unikt namn — används som XML-rotelementnamn och i ETERNA
  label:
    sv: "Ärende"               # Visningsnamn i svenska
    en: "Case"                 # Visningsnamn på engelska

# item = ett objekt inuti record (t.ex. handling, fotografi, dokument)
item:
  metadataType: "min-handling"
  label:
    sv: "Handling"
    en: "Document"

recordFields:                  # Fält för record-typen
  - name: fältnamn             # XML-elementnamn — inga mellanslag, helst ASCII
    type: string               # Se fälttyper nedan
    required: true             # true = obligatoriskt, false = valfritt
    label:
      sv: "Visningsnamn"
      en: "Display Name"

  - name: status
    type: enumeration
    required: false
    values: ["Aktiv", "Avslutad"]  # Obligatorisk för enumeration-typ

itemFields:
  # Samma struktur som recordFields
```

#### Fälttyper

| Typ | Beskrivning | Validering | Solr-suffix |
|---|---|---|---|
| `string` | Kort text (en rad) | — | `_txt` |
| `text` | Lång text (flerrad) | — | `_txt` |
| `date` | Datum | YYYY-MM-DD | `_dt` |
| `datetime` | Datum och tid | ISO 8601 | `_dt` |
| `integer` | Heltal | Numerisk | `_txt` |
| `decimal` | Decimaltal | Numerisk | `_txt` |
| `bool` | Booleskt värde | `true`/`false` | `_txt` |
| `email` | E-postadress | — | `_txt` |
| `url` | URL | — | `_txt` |
| `enumeration` | Fördefinierade val | Måste finnas i `values` | `_txt` |

---

## REST API-referens

Bas-URL: `http://localhost:8082`

### POST `/api/records` — Skicka post för arkivering

Paketerar metadata och filer som ett E-ARK SIP och startar ett ingest-jobb i ETERNA.

**Request body:**

```json
{
  "parentId": "550e8400-e29b-41d4-a716-446655440000",
  "recordType": "RECORD",
  "fields": {
    "arendenummer": "2024-042",
    "titel": "Bygglov Storgatan 5",
    "oppningsdatum": "2024-03-01",
    "arkiveringsdatum": "2024-12-31"
  },
  "files": [
    {
      "filename": "ansökan.pdf",
      "base64Data": "JVBERi0xLjQK..."
    }
  ]
}
```

| Fält | Typ | Krav | Beskrivning |
|---|---|---|---|
| `parentId` | string | Ja | UUID till mål-AIP i ETERNA |
| `recordType` | `RECORD` \| `ITEM` | Ja | `RECORD` = behållare, `ITEM` = underobjekt |
| `fields` | object | Ja | Nyckel-värde-par enligt schema.yaml |
| `files` | array | Nej | Bifogade filer, base64-kodade (max ~10 MB/fil) |

**Response 202 Accepted:**

```json
{ "jobId": "abc-def-123" }
```

**Response 400 Bad Request (valideringsfel):**

```json
{
  "error": "Valideringsfel",
  "details": [
    "arende: obligatoriskt fält saknas: arendenummer",
    "arende: fält 'oppningsdatum' är inte ett giltigt datum (YYYY-MM-DD): 2024/03/01"
  ]
}
```

---

### GET `/api/records/{jobId}` — Hämta ingest-status

Returnerar status för ett pågående eller avslutat ingest-jobb.

**Response 200 OK:**

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
| `CREATED` | Jobb skapat, väntar på att starta |
| `STARTED` | Pågår |
| `COMPLETED` | Lyckades |
| `COMPLETED_WITH_PROBLEMS` | Lyckades med varningar |
| `FAILED_DURING_INGEST` | Misslyckades under ingest |
| `FAILED` | Misslyckades |
| `STOPPED` | Avbrutet |

> **OBS:** `aipId` är alltid `null` i nuläget. Hämta AIP-ID via ETERNAs egna API efter `COMPLETED`.

---

### GET `/api/schema` — Hämta JSON Schema för UI

Returnerar schemat i JSON Schema-format (Draft-07) som React-UI:t använder för att rendera formulär dynamiskt.

**Response 200 OK:**

```json
{
  "record": {
    "metadataType": "arende",
    "label": { "sv": "Ärende", "en": "Case" },
    "schema": {
      "$schema": "http://json-schema.org/draft-07/schema#",
      "type": "object",
      "required": ["arendenummer", "titel"],
      "properties": {
        "arendenummer": { "type": "string", "title": "Ärendenummer" },
        "titel":        { "type": "string", "title": "Titel" },
        "status":       { "type": "string", "enum": ["Öppen", "Avslutad"] }
      }
    }
  },
  "item": { ... }
}
```

---

## Hjälpfunktioner — eterna-sip-commons

`eterna-sip-commons` är ett Spring Boot auto-konfigurerat bibliotek som exponerar tre kärnkomponenter.

### EternaClient

Kommunicerar med ETERNAs REST API. Konfigureras via Spring Boot-properties.

```java
@Autowired EternaClient eternaClient;

// Ladda upp en ZIP-fil till ETERNA Transfer
TransferResource transfer = eternaClient.uploadZip("min-sip.zip", zipPath);

// Starta ett ingest-jobb
IngestJob job = eternaClient.createJob(
    List.of(transfer.transferId()),
    IngestOptions.defaults(parentAipId)
);

// Hämta jobbstatus
IngestJob status = eternaClient.getJob(job.id());

// Kontrollera om AIP finns
boolean exists = eternaClient.aipExists("uuid-till-aip");

// Ta bort en transfer-resurs (städning vid fel)
eternaClient.deleteTransfer(transfer.transferId());
```

**Spring Boot properties (`application.yaml`):**

```yaml
eterna:
  url: http://eterna:8080
  username: admin
  password: eterna
  api-path: /api/v2
  read-timeout: 120s
  write-timeout: 600s
  extra-headers:             # Valfria extra HTTP-headers
    X-Custom-Header: värde
```

---

### IngestService

Orkestreringslagret ovanpå `EternaClient` — laddar upp SIP, startar jobb och pollar tills det är klart.

```java
@Autowired IngestService ingestService;

// Hela flödet i ett anrop (blockerande)
IngestResult result = ingestService.ingest(
    "min-sip.zip",
    Path.of("/tmp/min-sip.zip"),
    IngestOptions.defaults("parent-aip-uuid")
);

if (result.isSuccess()) {
    IngestResult.Success ok = (IngestResult.Success) result;
    System.out.println("Jobb-ID: " + ok.jobId());
} else {
    IngestResult.Failure fail = (IngestResult.Failure) result;
    System.out.println("Fel: " + fail.errorMessage());
}

// Polla ett befintligt jobb manuellt
IngestResult status = ingestService.awaitCompletion(
    "job-id",
    Duration.ofSeconds(10),   // Poll-intervall
    Duration.ofMinutes(60)    // Timeout
);
```

**IngestOptions:**

```java
// Standardinställningar (rekommenderat)
IngestOptions opts = IngestOptions.defaults("parent-aip-uuid");

// Anpassade inställningar
IngestOptions opts = new IngestOptions(
    "parent-aip-uuid",
    "org.roda.core.plugins.base.ingest.v2.ConfigurableIngestPlugin",
    false,   // virusCheck
    true,    // formatIdentification
    true,    // metadataValidation
    true,    // producerAuthCheck
    true,    // applyDisposalRules
    true,    // createSubmission
    true,    // forceParentId
    8,       // totalSteps
    "admin@example.com"  // emailNotification (null = ingen e-post)
);
```

---

### SipPackager

Bygger ett E-ARK SIP ZIP från metadata-XML och filer.

```java
SipPackager packager = new SipPackager();

// Bygg en SIP ZIP
Path sipZip = packager.buildZip(
    "unik-sip-id",           // Används som SIP-identifierare i METS
    metadataXmlPath,         // Path till metadata-XML-filen
    "arende",                // Metadata-typ (matchar schema.yaml metadataType)
    List.of(                 // Innehållsfiler
        new SipFile("ansökan.pdf", inputStream)
    ),
    workDir                  // Temporär katalog för bygget
);
// sipZip pekar på ZIP-filen — anroparen ansvarar för cleanup
```

**SipFile:**

```java
// Från fil
new SipFile("fil.pdf", Files.newInputStream(path))

// Från byte-array
new SipFile("fil.pdf", new ByteArrayInputStream(bytes))
```

---

## ETERNA-integration: overlay-systemet

Tjänsten genererar automatiskt de filer ETERNA behöver för att förstå er custom metadata-typ. Dessa kallas *overlay-filer* och placeras i ETERNAs config-katalog.

### Genererade filer

```
overlay/
├── schemas/
│   ├── arende.xsd           # XSD-schema — ETERNA validerar XML mot denna
│   └── handling.xsd
├── crosswalks/
│   ├── ingest/
│   │   ├── arende.xslt      # Mappar XML → Solr-fält (indexering)
│   │   └── handling.xslt
│   └── dissemination/html/
│       ├── arende.xslt      # Renderar metadata som HTML i ETERNA UI
│       └── handling.xslt
├── templates/
│   ├── arende.xml.hbs       # Handlebars-mall för ETERNAs metadata-editor
│   └── handling.xml.hbs
└── i18n/
    ├── ServerMessages.properties       # Engelska fältnamn
    └── ServerMessages_sv_SE.properties # Svenska fältnamn
```

### Installera overlay i ETERNA

```bash
# 1. Kopiera overlay-filer
docker compose cp eterna-ingest-service:/config/overlay/. /path/to/eterna/config/

# 2. Starta om ETERNA
docker compose restart eterna

# 3. Verifiera — ny metadata-typ ska synas i ETERNAs ingest-konfiguration
```

### Solr-indexering

Ingest-XSLT mappar fälten till Solrs dynamiska fältmönster:
- `date`/`datetime`-fält → `fältnamn_dt` (sökbar som datum)
- Övriga fält → `fältnamn_txt` (fulltext-sökbar)

---

## Exempelscheman

| Fil | Metadata-typ | Användningsfall |
|---|---|---|
| [`examples/schema-arende.yaml`](examples/schema-arende.yaml) | `arende` + `handling` | Ärendehantering för kommuner/myndigheter |
| [`examples/schema-ead-2002.yaml`](examples/schema-ead-2002.yaml) | `ead_2002` | Arkivförteckning enligt EAD 2002 / ISAD(G) |
| [`examples/schema-ead-3.yaml`](examples/schema-ead-3.yaml) | `ead_3` | Arkivförteckning enligt EAD 3 (modernare standard) |

EAD-schemana använder exakt samma fältnamn som ETERNAs inbyggda EAD-mallar. Se kommentarerna i respektive fil för information om metadatatyp-konflikter om ni även ingesta äkta EAD-SIPar direkt i ETERNA.

### Eget schema — kom igång

1. Kopiera ett exempelschema: `cp examples/schema-arende.yaml min-schema.yaml`
2. Sätt `INGEST_SCHEMA_PATH=min-schema.yaml`
3. Starta tjänsten — overlay genereras automatiskt
4. Installera overlay i ETERNA och starta om
5. Testa via Swagger UI på http://localhost:8082/swagger-ui.html

---

## Bygga lokalt

**Förutsättningar:** Java 21, Maven 3.9+, Node 20+, GitHub-token med `read:packages`

```bash
# Maven-modulerna
mvn clean package -DskipTests

# Kör tjänsten mot ett eget schema
cd eterna-ingest-service
INGEST_SCHEMA_PATH=../examples/schema-arende.yaml \
ETERNA_URL=http://localhost:8080 \
java -jar target/eterna-ingest-service-*.jar
```

### Använda eterna-sip-commons som bibliotek

Lägg till beroendet i er `pom.xml`:

```xml
<dependency>
    <groupId>se.eterna</groupId>
    <artifactId>eterna-sip-commons</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Spring Boot auto-konfigurerar `EternaClient` och `IngestService` automatiskt när ni sätter `eterna.*`-properties. `SipPackager` skapas som en Spring Bean och kan injiceras direkt.

---

## Arkitektur

```
schema.yaml (kundkonfiguration)
      │
      ▼
eterna-ingest-service (Spring Boot :8082)
  ├── POST /api/records          ← Ta emot metadata + filer
  ├── GET  /api/records/{jobId}  ← Polla ingest-status
  ├── GET  /api/schema           ← JSON Schema för UI-rendering
  └── OverlayGenerator           ← Genererar ETERNA config-filer vid startup
      │
      ▼
eterna-sip-commons
  ├── SchemaValidator    (validerar fältvärden mot schema.yaml)
  ├── MetadataXmlGenerator (skapar XML från fältvärden)
  ├── SipPackager        (paketerar E-ARK SIP via commons-ip2)
  └── EternaClient / IngestService (REST-kommunikation + polling)
      │
      ▼
ETERNA (digital bevaranderepository)
  ├── Transfer API       ← Mottar SIP ZIP
  ├── Jobs API           ← Kör ConfigurableIngestPlugin
  └── AIP                ← Resultatet: arkiverat informationspaket
```
