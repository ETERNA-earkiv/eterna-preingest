package se.eterna.ingest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Base64;
import java.util.List;
import java.util.Map;

public record SubmitRecordRequest(
    @NotBlank String parentId,
    @NotNull RecordType recordType,
    @NotNull Map<String, String> fields,
    List<FileAttachment> files
) {
    public enum RecordType { RECORD, ITEM }

    public record FileAttachment(
        @NotBlank String filename,
        // Base64-kodad fildata för filer under 10 MB
        String base64Data
    ) {
        public byte[] decodedData() {
            return Base64.getDecoder().decode(base64Data);
        }
    }
}
