package se.eterna.ingest.service;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import se.eterna.commons.client.EternaClient;
import se.eterna.commons.client.IngestJob;
import se.eterna.commons.client.TransferResource;
import se.eterna.commons.ingest.IngestOptions;
import se.eterna.commons.sip.SipFile;
import se.eterna.commons.sip.SipPackager;
import se.eterna.ingest.config.SchemaDefinition;
import se.eterna.ingest.config.SchemaLoader;
import se.eterna.ingest.dto.RecordStatusResponse;
import se.eterna.ingest.dto.SubmitRecordRequest;
import se.eterna.ingest.dto.SubmitRecordResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RecordService {

    private static final Logger log = LoggerFactory.getLogger(RecordService.class);

    private final EternaClient eternaClient;
    private final SchemaLoader schemaLoader;
    private final SchemaValidator validator;
    private final MetadataXmlGenerator xmlGenerator;
    private final Ead3XmlGenerator ead3XmlGenerator;
    private final Ead3Service ead3Service;
    private final SipPackager sipPackager;

    @Value("${ingest.work-dir:/tmp/eterna-ingest}")
    private String workDirBase;

    public RecordService(
            EternaClient eternaClient,
            SchemaLoader schemaLoader,
            SchemaValidator validator,
            MetadataXmlGenerator xmlGenerator,
            Ead3XmlGenerator ead3XmlGenerator,
            Ead3Service ead3Service,
            SipPackager sipPackager
    ) {
        this.eternaClient = eternaClient;
        this.schemaLoader = schemaLoader;
        this.validator = validator;
        this.xmlGenerator = xmlGenerator;
        this.ead3XmlGenerator = ead3XmlGenerator;
        this.ead3Service = ead3Service;
        this.sipPackager = sipPackager;
    }

    public SubmitRecordResponse submit(SubmitRecordRequest request) throws Exception {
        SchemaDefinition schema = schemaLoader.getSchema();
        boolean isItem = request.recordType() == SubmitRecordRequest.RecordType.ITEM;

        List<SchemaDefinition.FieldDefinition> fieldDefs = isItem
            ? schema.itemFields() : schema.recordFields();
        SchemaDefinition.TypeConfig typeConfig = isItem ? schema.item() : schema.record();

        // Validera fält mot schema
        List<String> errors = validator.validate(request.fields(), fieldDefs,
            typeConfig != null ? typeConfig.metadataType() : "record");
        List<String> realErrors = errors.stream()
            .filter(e -> !e.contains("okänt fält ignoreras"))
            .toList();
        if (!realErrors.isEmpty()) {
            throw new ValidationException(realErrors);
        }

        Path workDir = Files.createTempDirectory(Path.of(workDirBase), "sip-");
        try {
            // Generera metadata-XML
            var metadataType = typeConfig != null ? typeConfig.metadataType() : "record";
            var rootElement = (typeConfig != null && typeConfig.rootElement() != null) ? typeConfig.rootElement() : metadataType;
            var wrapperElement = (typeConfig != null && typeConfig.rootElement() != null && typeConfig.wrapperElement() != null) ? typeConfig.wrapperElement() : null;
            var namespace = typeConfig != null ? typeConfig.namespace() : null;
            Path metadataFile;
            if (metadataType.equals("ead_3")) {
                metadataFile = generateEad3Xml(request.fields(), workDir);
            } else {
                metadataFile = xmlGenerator.generate(
                        rootElement, wrapperElement, namespace, request.fields(), fieldDefs, workDir, metadataType
                );
            }

            // Förbered bifogade filer
            List<SipFile> sipFiles = buildSipFiles(request);

            // Bygg SIP ZIP
            String sipId = UUID.randomUUID().toString();
            Path zipPath = sipPackager.buildZip(sipId, Map.of(metadataType, metadataFile), sipFiles, workDir);

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

    private Path generateEad3Xml(@NotNull Map<String, String> fields, Path workDir) throws JAXBException, SAXException {
        var title = fields.get("title");
        var recordId = UUID.randomUUID().toString();
        var agencyName = fields.get("agencyname");
        var archivalDescriptionParameters = new Ead3Service.ArchivalDescriptionParameters(
                fields.get("level"),
                fields.get("refname"),
                fields.get("refid"),
                fields.get("relatedmaterial"),
                fields.get("securityclass"),
                fields.get("classificationstructure"),
                fields.get("classificationstructureversion"),
                fields.get("structuralunit"),
                fields.get("structuralunitdenotation"),
                fields.get("processinfo")
        );
        var ead3 = ead3Service.createEad3(title, recordId, agencyName, archivalDescriptionParameters);
        Path xmlFilePath = workDir.resolve("ead3.xml");

        ead3XmlGenerator.generate(ead3, xmlFilePath);

        return xmlFilePath;
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

    private List<SipFile> buildSipFiles(SubmitRecordRequest request) {
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
