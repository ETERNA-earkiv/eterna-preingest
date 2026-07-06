package se.eterna.ingest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Base64;
import java.util.List;
import java.util.Map;

public record SubmitRecordV2Request(
    @NotBlank String parentId,
    @NotNull Map<String, Map<String, String>> fieldsMap,
    List<FileAttachment> files
) {

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
