package se.eterna.ingest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class SchemaLoader {

    private static final Logger log = LoggerFactory.getLogger(SchemaLoader.class);

    @Value("${ingest.schema-path:/config/schema.yaml}")
    private String schemaPath;

    private SchemaDefinition schema;

    @PostConstruct
    public void load() throws Exception {
        Path path = Path.of(schemaPath);
        if (!Files.exists(path)) {
            throw new IllegalStateException(
                "schema.yaml saknas: " + path.toAbsolutePath() +
                "\nMonteras via: -v /din/schema.yaml:" + schemaPath
            );
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
            .findAndRegisterModules();
        schema = mapper.readValue(path.toFile(), SchemaDefinition.class);
        log.info("Schema laddat: metadataType={}, recordFields={}, itemFields={}, rootElement={}, wrapperElement={}, namespace={}",
            schema.record() != null ? schema.record().metadataType() : "?",
            schema.recordFields() != null ? schema.recordFields().size() : 0,
            schema.itemFields() != null ? schema.itemFields().size() : 0,
            schema.record() != null ? schema.record().rootElement() : "?",
            schema.record() != null ? schema.record().wrapperElement() : "?",
            schema.record() != null ? schema.record().namespace() : "?");
    }

    public SchemaDefinition getSchema() {
        return schema;
    }
}
