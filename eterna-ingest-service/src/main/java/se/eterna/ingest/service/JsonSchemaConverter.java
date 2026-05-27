package se.eterna.ingest.service;

import org.springframework.stereotype.Component;
import se.eterna.ingest.config.SchemaDefinition;
import se.eterna.ingest.config.SchemaDefinition.FieldDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Konverterar SchemaDefinition till JSON Schema som React-UI:t konsumerar
 * för dynamisk formulärsrendering.
 */
@Component
public class JsonSchemaConverter {

    public Map<String, Object> convert(SchemaDefinition schema) {
        Map<String, Object> result = new HashMap<>();

        if (schema.record() != null) {
            result.put("record", Map.of(
                "metadataType", schema.record().metadataType(),
                "label", labelMap(schema.record().label()),
                "schema", buildJsonSchema(schema.recordFields())
            ));
        }

        if (schema.item() != null) {
            result.put("item", Map.of(
                "metadataType", schema.item().metadataType(),
                "label", labelMap(schema.item().label()),
                "schema", buildJsonSchema(schema.itemFields())
            ));
        }

        return result;
    }

    private Map<String, Object> buildJsonSchema(List<FieldDefinition> fields) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");

        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        if (fields != null) {
            for (FieldDefinition f : fields) {
                properties.put(f.name(), fieldSchema(f));
                if (f.required()) required.add(f.name());
            }
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) schema.put("required", required);
        return schema;
    }

    private Map<String, Object> fieldSchema(FieldDefinition f) {
        Map<String, Object> prop = new HashMap<>();

        switch (f.type()) {
            case string, text, email, url -> prop.put("type", "string");
            case integer -> prop.put("type", "integer");
            case decimal -> prop.put("type", "number");
            case bool -> prop.put("type", "boolean");
            case date -> {
                prop.put("type", "string");
                prop.put("format", "date");
            }
            case datetime -> {
                prop.put("type", "string");
                prop.put("format", "date-time");
            }
            case enumeration -> {
                prop.put("type", "string");
                prop.put("enum", f.values());
            }
        }

        // Sätt titel från label (prioritera svenska)
        if (f.label() != null) {
            String title = f.label().sv() != null ? f.label().sv() : f.label().en();
            if (title != null) prop.put("title", title);
        } else {
            prop.put("title", f.name());
        }

        return prop;
    }

    private Map<String, String> labelMap(SchemaDefinition.Label label) {
        if (label == null) return Map.of();
        Map<String, String> m = new HashMap<>();
        if (label.sv() != null) m.put("sv", label.sv());
        if (label.en() != null) m.put("en", label.en());
        return m;
    }
}
