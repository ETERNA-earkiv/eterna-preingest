package se.eterna.ingest.service;

import org.springframework.stereotype.Component;
import se.eterna.ingest.config.SchemaDefinition;
import se.eterna.ingest.config.SchemaDefinition.FieldDefinition;
import se.eterna.ingest.config.SchemaV2Definition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Konverterar SchemaDefinition till JSON Schema som React-UI:t konsumerar
 * för dynamisk formulärsrendering.
 */
@Component
public class JsonSchemaV2Converter {

    public List<List<Map<String, Object>>> convert(SchemaV2Definition schema) {
        List<List<Map<String, Object>>> result = new ArrayList<>();

        if (schema.recordsList() != null) {
            schema.recordsList().forEach(records -> {
                List<Map<String, Object>> recordMapList = new ArrayList<>();
                records.records().forEach(record -> {
                    Map<String, Object> recordMap = new HashMap<>();
                    recordMap.put("metadataType", record.metadataType());
                    if (record.rootElement() != null) {
                        recordMap.put("rootElement", record.rootElement());
                    }
                    if (record.wrapperElement() != null) {
                        recordMap.put("wrapperElement", record.wrapperElement());
                    }
                    if (record.namespace() != null) {
                        recordMap.put("namespace", record.namespace());
                    }
                    recordMap.put("label", labelMap(record.label()));
                    recordMap.put("schema", buildJsonSchema(record.fields()));
                    recordMapList.add(recordMap);
                });
                result.add(recordMapList);
            });
        }

        return result;
    }

    private Map<String, Object> buildJsonSchema(List<SchemaV2Definition.FieldDefinition> fields) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");

        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        if (fields != null) {
            for (SchemaV2Definition.FieldDefinition f : fields) {
                properties.put(f.name(), fieldSchema(f));
                if (f.required()) required.add(f.name());
            }
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) schema.put("required", required);
        return schema;
    }

    private Map<String, Object> fieldSchema(SchemaV2Definition.FieldDefinition f) {
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

    private Map<String, String> labelMap(SchemaV2Definition.Label label) {
        if (label == null) return Map.of();
        Map<String, String> m = new HashMap<>();
        if (label.sv() != null) m.put("sv", label.sv());
        if (label.en() != null) m.put("en", label.en());
        return m;
    }
}
