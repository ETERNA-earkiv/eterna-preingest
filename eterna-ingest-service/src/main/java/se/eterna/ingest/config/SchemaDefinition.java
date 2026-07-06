package se.eterna.ingest.config;

import java.util.List;

/**
 * Modell för schema.yaml — styr vilka fält som accepteras i API och UI.
 */
public record SchemaDefinition(
    TypeConfig record,
    TypeConfig item,
    List<FieldDefinition> recordFields,
    List<FieldDefinition> itemFields
) {
    public record TypeConfig(String metadataType, Label label, String rootElement, String wrapperElement, String namespace) {}

    public record Label(String sv, String en) {
        public String forLocale(String lang) {
            return "sv".equals(lang) ? sv : en;
        }
    }

    public record FieldDefinition(
        String name,
        FieldType type,
        boolean required,
        Label label,
        List<String> values  // För enum-typ
    ) {
        public FieldDefinition {
            if (values == null) values = List.of();
        }
    }

    public enum FieldType {
        string, text, date, datetime, integer, decimal, bool, email, url, enumeration
    }

    public FieldDefinition findRecordField(String name) {
        if (recordFields == null) return null;
        return recordFields.stream().filter(f -> f.name().equals(name)).findFirst().orElse(null);
    }

    public FieldDefinition findItemField(String name) {
        if (itemFields == null) return null;
        return itemFields.stream().filter(f -> f.name().equals(name)).findFirst().orElse(null);
    }
}
