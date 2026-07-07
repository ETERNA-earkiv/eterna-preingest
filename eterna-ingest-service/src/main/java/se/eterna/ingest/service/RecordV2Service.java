package se.eterna.ingest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.eterna.commons.client.EternaClient;
import se.eterna.commons.client.IngestJob;
import se.eterna.commons.client.TransferResource;
import se.eterna.commons.ingest.IngestOptions;
import se.eterna.commons.sip.SipFile;
import se.eterna.commons.sip.SipPackager;
import se.eterna.ingest.config.SchemaV2Definition;
import se.eterna.ingest.config.SchemaV2Loader;
import se.eterna.ingest.dto.RecordStatusResponse;
import se.eterna.ingest.dto.SubmitRecordRequest;
import se.eterna.ingest.dto.SubmitRecordResponse;
import se.eterna.ingest.dto.SubmitRecordV2Request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class RecordV2Service {

    private static final Logger log = LoggerFactory.getLogger(RecordV2Service.class);

    private final EternaClient eternaClient;
    private final SchemaV2Loader schemaLoader;
    private final SchemaValidator validator;
    private final MetadataXmlV2Generator xmlGenerator;
    private final SipPackager sipPackager;

    @Value("${ingest.work-dir:/tmp/eterna-ingest}")
    private String workDirBase;

    public RecordV2Service(
        EternaClient eternaClient,
        SchemaV2Loader schemaLoader,
        SchemaValidator validator,
        MetadataXmlV2Generator xmlGenerator,
        SipPackager sipPackager
    ) {
        this.eternaClient = eternaClient;
        this.schemaLoader = schemaLoader;
        this.validator = validator;
        this.xmlGenerator = xmlGenerator;
        this.sipPackager = sipPackager;
    }

    public SubmitRecordResponse submit(SubmitRecordV2Request request) throws Exception {
        SchemaV2Definition schema = schemaLoader.getSchema();

//        List<SchemaDefinition.FieldDefinition> fieldDefs = isItem
//            ? schema.itemFields() : schema.recordFields();
//        SchemaDefinition.TypeConfig typeConfig = isItem ? schema.item() : schema.record();

        // Validera fält mot schema
//        List<String> errors = validator.validate(request.fields(), fieldDefs,
//            typeConfig != null ? typeConfig.metadataType() : "record");
//        List<String> realErrors = errors.stream()
//            .filter(e -> !e.contains("okänt fält ignoreras"))
//            .toList();
//        if (!realErrors.isEmpty()) {
//            throw new ValidationException(realErrors);
//        }

        Path workDir = Files.createTempDirectory(Path.of(workDirBase), "sip-");
        try {

            var metadataMap = new HashMap<String, Path>();

            request.fieldsMap().forEach((requestMetadataType, requestFields) -> {
                Optional<SchemaV2Definition.TypeConfig> typeConfigOpt = schema.recordsList().stream()
                        .flatMap(recordGroup -> recordGroup.records().stream())
                        .filter(typeConfig -> requestMetadataType.equals(typeConfig.metadataType()))
                        .findFirst();

                if (typeConfigOpt.isPresent()) {
                    var typeConfig = typeConfigOpt.get();

                    // Generera metadata-XML
                    var metadataType = typeConfig.metadataType();
                    var rootElement = typeConfig.rootElement() != null ? typeConfig.rootElement() : metadataType;
                    var wrapperElement = typeConfig.rootElement() != null && typeConfig.wrapperElement() != null ? typeConfig.wrapperElement() : null;
                    var namespace = typeConfig.namespace();
                    try {
                        metadataMap.put(metadataType, xmlGenerator.generate(
                                rootElement, wrapperElement, namespace, requestFields, typeConfig.fields(), workDir, metadataType
                        ));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            // Förbered bifogade filer
            List<SipFile> sipFiles = buildSipFiles(request);

            // Bygg SIP ZIP
            String sipId = UUID.randomUUID().toString();
            Path zipPath = sipPackager.buildZip(sipId, metadataMap, sipFiles, workDir);

            // Ladda upp till ETERNA
            {
                TransferResource transfer = eternaClient.uploadZip(sipId + ".zip", zipPath);
                log.info("SIP uppladdad: transferId={}", transfer.transferId());

                // Starta ingest-jobb
                IngestJob job = eternaClient.createJob(
                    List.of(transfer.transferId()),
                    IngestOptions.defaults(request.parentId())
                );
                log.info("Ingest-jobb startat: jobId={}", job.id());
                return new SubmitRecordResponse(job.id());
            }
        } finally {
            // Städa upp temporära filer
            deleteDir(workDir);
        }
    }

    public RecordStatusResponse getStatus(String jobId) {
        IngestJob job = eternaClient.getJob(jobId);
        return new RecordStatusResponse(
            job.id(),
            job.state(),
            job.percentageCompleted(),
            null  // AIP-ID extraheras ur ETERNA-rapporter efter COMPLETED
        );
    }

    private List<SipFile> buildSipFiles(SubmitRecordV2Request request) {
        if (request.files() == null) return List.of();
        List<SipFile> result = new ArrayList<>();
        for (var attachment : request.files()) {
            if (attachment.base64Data() != null) {
                byte[] data = attachment.decodedData();
                result.add(new SipFile(attachment.filename(), new ByteArrayInputStream(data)));
            }
        }
        return result;
    }

    private void deleteDir(Path dir) {
        try {
            try (var walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); }
                        catch (IOException ignored) {}
                    });
            }
        } catch (IOException e) {
            log.warn("Kunde inte rensa temporär katalog: {}", dir, e);
        }
    }
}
