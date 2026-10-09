package se.eterna.ingest.config;

import java.util.List;

/**
 * Modell för schema.yaml — styr vilka fält som accepteras i API och UI.
 */
public record SchemaDefinition(
        List<SchemaDefinition.RecordGroup> recordsList
) {
    public record RecordGroup(
            List<SchemaDefinition.TypeConfig> records
    ) {
    }

    public record TypeConfig(
            String metadataType,
            Label label,
            String rootElement,
            String wrapperElement,
            String namespace,
            List<FieldDefinition> fields
    ) {
    }

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
}
