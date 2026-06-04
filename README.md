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

---

## eterna-sip-commons — biblioteksreferens

`eterna-sip-commons` är ett Spring Boot auto-konfigurerat bibliotek. Lägg till det i er `pom.xml`:

```xml
<dependency>
    <groupId>se.eterna</groupId>
    <artifactId>eterna-sip-commons</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Spring Boot auto-konfigurerar alla beans automatiskt när `eterna.url` är satt i properties.

### Paketstruktur

```
se.eterna.commons
├── client/          EternaClient, ApiResult, NamedInputStreamResource
├── ingest/          IngestService, BatchIngestService, IngestOptions, IngestResult
├── sip/             SipPackager, SipFile, SipOutputTarget, FileOutputTarget, EternaUploadTarget
│   └── domain/      ArchivableRecord, RecordSipBuilder
├── io/              DataBufferOutputStream, DataBufferInputStream, StreamProcessor, StreamPipeline
├── xml/             StaxXmlValidator
└── exception/       EternaClientException, SipBuildException, XmlValidationException, IngestTimeoutException
```

---

### EternaClient

Kommunicerar med ETERNAs REST API.

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
  extra-headers:
    X-Custom-Header: värde
```

**ApiResult — typsäker felhantering:**

```java
// Förväntas i framtida version av EternaClient
ApiResult<TransferResource> result = eternaClient.tryUploadZip("min-sip.zip", zipPath);
switch (result) {
    case ApiSuccess<TransferResource> ok  -> startJob(ok.value());
    case ApiError<TransferResource>   err -> log.error("Upload misslyckades: {}", err.message());
}
```

---

### IngestService

Orkestreringslagret — laddar upp SIP, startar jobb och pollar tills det är klart.

```java
@Autowired IngestService ingestService;

// Hela flödet i ett anrop (blockerande)
IngestResult result = ingestService.ingest(
    "min-sip.zip",
    Path.of("/tmp/min-sip.zip"),
    IngestOptions.defaults("parent-aip-uuid")
);

switch (result) {
    case IngestResult.Success ok   -> System.out.println("Klar: " + ok.jobId());
    case IngestResult.Failure fail -> System.out.println("Fel: " + fail.errorMessage());
}

// Polla ett befintligt jobb manuellt
IngestResult status = ingestService.awaitCompletion(
    "job-id",
    Duration.ofSeconds(10),   // Poll-intervall
    Duration.ofMinutes(60)    // Timeout — kastar IngestTimeoutException om timeout uppnås
);
```

**IngestOptions med retry:**

```java
// Standardinställningar — 3 retry-försök med 5 sekunders väntan
IngestOptions opts = IngestOptions.defaults("parent-aip-uuid");

// Utan retry
IngestOptions opts = IngestOptions.noRetry("parent-aip-uuid");

// Anpassade inställningar
IngestOptions opts = new IngestOptions(
    "parent-aip-uuid",
    "org.roda.core.plugins.base.ingest.v2.ConfigurableIngestPlugin",
    false,               // virusCheck
    true,                // formatIdentification
    true,                // metadataValidation
    true,                // producerAuthCheck
    true,                // applyDisposalRules
    true,                // createSubmission
    true,                // forceParentId
    8,                   // totalSteps
    "admin@example.com", // emailNotification (null = ingen e-post)
    5,                   // maxRetries — antal omförsök vid upload-fel
    Duration.ofSeconds(10) // retryDelay — väntetid mellan försök
);
```

---

### BatchIngestService

Ingestas en lista med SIP-paket sekventiellt. Fortsätter även om enstaka jobb misslyckas.

```java
@Autowired BatchIngestService batchIngestService;

List<BatchIngestService.SipJob> jobs = List.of(
    new BatchIngestService.SipJob("arende-001.zip", path1, IngestOptions.defaults(parentId)),
    new BatchIngestService.SipJob("arende-002.zip", path2, IngestOptions.defaults(parentId)),
    new BatchIngestService.SipJob("arende-003.zip", path3, IngestOptions.defaults(parentId))
);

List<IngestResult> results = batchIngestService.ingestAll(jobs);

long successes = results.stream().filter(IngestResult::isSuccess).count();
System.out.println(successes + "/" + results.size() + " lyckades");
```

---

### SipPackager

Bygger ett E-ARK SIP ZIP från metadata-XML och filer.

```java
SipPackager packager = new SipPackager();

Path sipZip = packager.buildZip(
    "unik-sip-id",          // SIP-identifierare i METS
    metadataXmlPath,        // Path till metadata-XML-filen
    "arende",               // Metadata-typ
    List.of(
        new SipFile("ansökan.pdf", inputStream)
    ),
    workDir                 // Temporär katalog
);
```

---

### ArchivableRecord och RecordSipBuilder

Domänvänligt API för att bygga SIP av poster och handlingar.

```java
@Autowired RecordSipBuilder recordSipBuilder;

ArchivableRecord post = ArchivableRecord.builder()
    .id("arende-2024-001")
    .metadataFile(metadataXmlPath)
    .metadataType("arende")
    .file(new SipFile("ansökan.pdf", stream))
    .file(new SipFile("bilaga.pdf", stream2))
    .build();

Path zipPath = recordSipBuilder.build(post, workDir);
// zipPath → skicka vidare med IngestService eller SipOutputTarget
```

---

### SipOutputTarget

Abstraktion för vad som händer med ett färdigbyggt SIP ZIP.

```java
// EternaUploadTarget (default) — laddar upp direkt till ETERNA
SipOutputTarget target = new EternaUploadTarget(eternaClient);
TransferResource transfer = target.complete("min-sip.zip", zipPath);

// FileOutputTarget — sparar till katalog (för testning/debug)
SipOutputTarget target = new FileOutputTarget(Path.of("/tmp/sips"));
target.complete("min-sip.zip", zipPath); // Kopierar filen, returnerar null

// Egen implementering
@Bean
public SipOutputTarget mySipOutputTarget(EternaClient client) {
    return (filename, path) -> {
        log.info("Uploading {}", filename);
        return client.uploadZip(filename, path);
    };
}
```

---

### IO-streaming

Reaktiva I/O-utilities för streaming av data utan in-memory-buffring.

```java
// DataBufferOutputStream — skriv till reaktiv Flux<DataBuffer>
Flux<DataBuffer> dataFlux = Flux.create(sink -> {
    DataBufferOutputStream out = new DataBufferOutputStream(bufferFactory, sink);
    // Skriv till out — data chunkas till sinken
    out.write(bytes);
    out.close();
    sink.complete();
});

// StreamPipeline — kör data genom en processor-kedja
StreamPipeline pipeline = new StreamPipeline();
pipeline.process(inputStream, outputStream,
    checksumProcessor.then(zipEntryProcessor));
```

---

### StaxXmlValidator

Streaming XML-validering mot ett XSD-schema utan in-memory-buffring.

```java
Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
    .newSchema(new File("schema.xsd"));

StaxXmlValidator validator = new StaxXmlValidator();
validator.start(schema);

// Mata in data i bitar (t.ex. från en reaktiv ström)
for (ByteBuffer chunk : xmlChunks) {
    validator.accept(chunk);
}

validator.finish(); // Blockerar och kastar XmlValidationException om ogiltig XML
```

---

### Undantagshierarki

```
RuntimeException
└── EternaClientException      — ETERNA API-kommunikationsfel
    └── IngestTimeoutException — Ingest-jobb avslutades inte i tid
SipBuildException              — E-ARK SIP-paketeringen misslyckades
XmlValidationException         — XML uppfyller inte XSD-schemat
```

```java
try {
    ingestService.ingest("sip.zip", path, options);
} catch (IngestTimeoutException e) {
    // Jobbet startade men slutfördes inte inom timeout
    log.warn("Timeout: {}", e.getMessage());
} catch (EternaClientException e) {
    // Nätverksfel, autentiseringsfel etc.
    log.error("ETERNA-fel: {}", e.getMessage(), e);
} catch (SipBuildException e) {
    // Fel vid paketering — metadata saknas, filsystemfel etc.
    log.error("SIP-fel: {}", e.getMessage(), e);
}
```

---

## ETERNA-integration: overlay-systemet

Tjänsten genererar automatiskt de filer ETERNA behöver för att förstå er custom metadata-typ. Dessa kallas *overlay-filer* och placeras i ETERNAs config-katalog.

### Genererade filer

```
overlay/
├── schemas/
│   ├── arende.xsd
│   └── handling.xsd
├── crosswalks/
│   ├── ingest/
│   │   ├── arende.xslt
│   │   └── handling.xslt
│   └── dissemination/html/
│       ├── arende.xslt
│       └── handling.xslt
├── templates/
│   ├── arende.xml.hbs
│   └── handling.xml.hbs
└── i18n/
    ├── ServerMessages.properties
    └── ServerMessages_sv_SE.properties
```

### Installera overlay i ETERNA

```bash
docker compose cp eterna-ingest-service:/config/overlay/. /path/to/eterna/config/
docker compose restart eterna
```

---

## Exempelscheman

| Fil | Metadata-typ | Användningsfall |
|---|---|---|
| [`examples/schema-arende.yaml`](examples/schema-arende.yaml) | `arende` + `handling` | Ärendehantering för kommuner/myndigheter |
| [`examples/schema-ead-2002.yaml`](examples/schema-ead-2002.yaml) | `ead_2002` | Arkivförteckning enligt EAD 2002 / ISAD(G) |
| [`examples/schema-ead-3.yaml`](examples/schema-ead-3.yaml) | `ead_3` | Arkivförteckning enligt EAD 3 |

---

## Bygga lokalt

**Förutsättningar:** Java 21, Maven 3.9+, Node 20+, GitHub-token med `read:packages`

```bash
mvn clean package -DskipTests
```

Kör mot eget schema:

```bash
cd eterna-ingest-service
INGEST_SCHEMA_PATH=../examples/schema-arende.yaml \
ETERNA_URL=http://localhost:8080 \
java -jar target/eterna-ingest-service-*.jar
```

---

## Arkitektur

```
schema.yaml (kundkonfiguration)
      │
      ▼
eterna-ingest-service (Spring Boot :8082)
  ├── POST /api/records
  ├── GET  /api/records/{jobId}
  ├── GET  /api/schema
  └── OverlayGenerator
      │
      ▼
eterna-sip-commons
  ├── exception/         EternaClientException, SipBuildException, XmlValidationException
  ├── client/            EternaClient, ApiResult<T>, NamedInputStreamResource
  ├── ingest/            IngestService (+ retry), BatchIngestService, IngestOptions, IngestResult
  ├── sip/               SipPackager, SipOutputTarget, EternaUploadTarget, FileOutputTarget
  │   └── domain/        ArchivableRecord, RecordSipBuilder
  ├── io/                DataBufferOutputStream, DataBufferInputStream, StreamProcessor
  └── xml/               StaxXmlValidator
      │
      ▼
ETERNA
  ├── Transfer API
  ├── Jobs API
  └── AIP
```
