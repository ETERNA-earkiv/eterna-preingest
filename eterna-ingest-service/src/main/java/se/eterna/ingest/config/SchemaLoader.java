package se.eterna.ingest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@Component
public class SchemaLoader {

    private static final Logger log = LoggerFactory.getLogger(SchemaLoader.class);

    @Value("${ingest.schema-path:/config/schema.yaml}")
    private String schemaPath;

    @Getter
    private SchemaDefinition schema;

    @PostConstruct
    public void load() throws IllegalStateException, IOException {
        Path path = Path.of(schemaPath);
        if (!Files.exists(path)) {
            throw new IllegalStateException(
                "schema.yaml saknas: " + path.toAbsolutePath() +
                "\nMonteras via: -v /din/schema.yaml:" + schemaPath
            );
        }
        Yaml snakeYaml = new Yaml();
        Object rawTree;
        try (InputStream in = Files.newInputStream(path)) {
            rawTree = snakeYaml.load(in);
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                .findAndRegisterModules();
        schema = mapper.convertValue(rawTree, SchemaDefinition.class);
        logSchemaDefinitions(schema);
    }

    private void logSchemaDefinitions(SchemaDefinition schema) {
        for (var recordGroup: schema.recordsList()) {
            var recordGroupString = new ArrayList<String>();
            var recordStrings = new ArrayList<String>();
            for (var schemaRecord: recordGroup.records()) {
                if (schemaRecord != null) {
                    recordGroupString.add(schemaRecord.metadataType());
                    recordStrings.add(String.format("Schema laddat i gruppen: metadataType=%s, recordFields=%d, rootElement=%s, wrapperElement=%s, namespace=%s",
                            schemaRecord.metadataType(),
                            schemaRecord.fields() != null ? schemaRecord.fields().size() : 0,
                            schemaRecord.rootElement(),
                            schemaRecord.wrapperElement(),
                            schemaRecord.namespace()));
                }
            }
            if (log.isInfoEnabled()) {
                log.info("Gruppschema laddat med metadatatyperna: {}.", String.join(", ", recordGroupString));
                for (var recordString: recordStrings) {
                    log.info(recordString);
                }
            }
        }
    }

}
