# eterna-sip-commons

Spring Boot auto-konfigurerat bibliotek med ETERNA REST-klient, E-ARK SIP-paketering och ingest-orkestrering.

Lägg till beroendet i er `pom.xml`:

```xml
<dependency>
    <groupId>se.eterna</groupId>
    <artifactId>eterna-sip-commons</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Spring Boot auto-konfigurerar alla beans automatiskt när `eterna.url` är satt i properties.

---

## Paketstruktur

```
se.eterna.commons
├── client/          EternaClient, ApiResult<T>, NamedInputStreamResource
├── ingest/          IngestService, BatchIngestService, IngestOptions, IngestResult
├── sip/             SipPackager, SipFile, SipOutputTarget, EternaUploadTarget, FileOutputTarget
│   └── domain/      ArchivableRecord, RecordSipBuilder
├── io/              DataBufferOutputStream, DataBufferInputStream, StreamProcessor, StreamPipeline
├── xml/             StaxXmlValidator
└── exception/       EternaClientException, SipBuildException, XmlValidationException, IngestTimeoutException
```

---

## Konfiguration

```yaml
eterna:
  url: http://eterna:8080          # Obligatorisk
  username: admin                  # Default: admin
  password: eterna                 # Default: eterna
  api-path: /api/v2                # Default: /api/v2
  read-timeout: 120s               # Default: 60s
  write-timeout: 600s              # Default: 60s
  verify-ssl: false                # Default: false
  extra-headers:                   # Valfria HTTP-headers
    X-Custom-Header: värde
```

---

## EternaClient

Kommunicerar direkt med ETERNAs REST API.

```java
@Autowired EternaClient eternaClient;

TransferResource transfer = eternaClient.uploadZip("min-sip.zip", zipPath);

IngestJob job = eternaClient.createJob(
    List.of(transfer.transferId()),
    IngestOptions.defaults(parentAipId)
);

IngestJob status = eternaClient.getJob(job.id());
boolean exists   = eternaClient.aipExists("uuid-till-aip");
eternaClient.deleteTransfer(transfer.transferId());
```

**ApiResult — typsäker felhantering:**

```java
switch (result) {
    case ApiSuccess<TransferResource> ok  -> startJob(ok.value());
    case ApiError<TransferResource>   err -> log.error("Fel: {}", err.message());
}
```

---

## IngestService

Laddar upp SIP, startar jobb och pollar tills det är klart.

```java
@Autowired IngestService ingestService;

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
    Duration.ofMinutes(60)    // Timeout — returnerar IngestResult.Failure vid timeout
);
```

**IngestOptions:**

```java
// Standardinställningar — 3 retry-försök med 5 sekunders väntan
IngestOptions opts = IngestOptions.defaults("parent-aip-uuid");

// Utan retry
IngestOptions opts = IngestOptions.noRetry("parent-aip-uuid");

// Anpassade inställningar
IngestOptions opts = new IngestOptions(
    "parent-aip-uuid",
    "org.roda.core.plugins.base.ingest.v2.ConfigurableIngestPlugin",
    false,                  // virusCheck
    true,                   // formatIdentification
    true,                   // metadataValidation
    true,                   // producerAuthCheck
    true,                   // applyDisposalRules
    true,                   // createSubmission
    true,                   // forceParentId
    8,                      // totalSteps
    "admin@example.com",    // emailNotification (null = ingen e-post)
    5,                      // maxRetries
    Duration.ofSeconds(10)  // retryDelay
);
```

---

## BatchIngestService

Ingestas en lista med SIP-paket sekventiellt. Fortsätter vid fel på enstaka jobb.

```java
@Autowired BatchIngestService batchIngestService;

List<BatchIngestService.SipJob> jobs = List.of(
    new BatchIngestService.SipJob("arende-001.zip", path1, IngestOptions.defaults(parentId)),
    new BatchIngestService.SipJob("arende-002.zip", path2, IngestOptions.defaults(parentId))
);

List<IngestResult> results = batchIngestService.ingestAll(jobs);
long successes = results.stream().filter(IngestResult::isSuccess).count();
```

---

## SipPackager

Bygger ett E-ARK SIP ZIP från metadata-XML och filer.

```java
Path sipZip = sipPackager.buildZip(
    "unik-sip-id",
    metadataXmlPath,
    "arende",
    List.of(new SipFile("ansökan.pdf", inputStream)),
    workDir
);
```

---

## ArchivableRecord och RecordSipBuilder

Domänvänligt API för att bygga SIP av poster och handlingar.

```java
@Autowired RecordSipBuilder recordSipBuilder;

ArchivableRecord post = ArchivableRecord.builder()
    .id("arende-2024-001")
    .metadataFile(metadataXmlPath)
    .metadataType("arende")
    .file(new SipFile("ansökan.pdf", stream))
    .build();

Path zipPath = recordSipBuilder.build(post, workDir);
```

---

## SipOutputTarget

Abstraktion för vart det färdiga SIP ZIP ska skickas.

```java
// EternaUploadTarget (default bean) — laddar upp direkt till ETERNA
SipOutputTarget target = new EternaUploadTarget(eternaClient);
TransferResource transfer = target.complete("min-sip.zip", zipPath);

// FileOutputTarget — sparar till katalog (testning/debug)
SipOutputTarget target = new FileOutputTarget(Path.of("/tmp/sips"));
target.complete("min-sip.zip", zipPath);

// Överskrid default bean med egen implementering
@Bean
public SipOutputTarget mySipOutputTarget(EternaClient client) {
    return (filename, path) -> client.uploadZip(filename, path);
}
```

---

## IO-streaming

Reaktiva I/O-utilities för streaming utan in-memory-buffring.

```java
// DataBufferOutputStream — skriv synkront, emittera som Flux<DataBuffer>
Flux<DataBuffer> dataFlux = Flux.create(sink -> {
    try (var out = new DataBufferOutputStream(bufferFactory, sink)) {
        out.write(bytes);
    }
    sink.complete();
});

// StreamPipeline — kör data genom en processor-kedja
new StreamPipeline().process(inputStream, outputStream,
    checksumProcessor.then(zipEntryProcessor));
```

---

## StaxXmlValidator

Streaming XML-validering mot XSD utan in-memory-buffring.

```java
Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
    .newSchema(new File("schema.xsd"));

StaxXmlValidator validator = new StaxXmlValidator();
validator.start(schema);

for (ByteBuffer chunk : xmlChunks) {
    validator.accept(chunk);
}
validator.finish(); // Kastar XmlValidationException om ogiltig XML
```

---

## Undantagshierarki

```
RuntimeException
└── EternaClientException      — ETERNA API-kommunikationsfel
    └── IngestTimeoutException — Reserv för klienter som vill kasta timeout som undantag
SipBuildException              — SIP-paketeringen misslyckades
XmlValidationException         — XML uppfyller inte XSD-schemat
```

```java
try {
    ingestService.ingest("sip.zip", path, options);
} catch (IngestTimeoutException e) {
    log.warn("Timeout: {}", e.getMessage());
} catch (EternaClientException e) {
    log.error("ETERNA-fel: {}", e.getMessage(), e);
} catch (SipBuildException e) {
    log.error("SIP-fel: {}", e.getMessage(), e);
}
```
