package se.eterna.commons.sip.domain;

import se.eterna.commons.sip.SipFile;

import java.nio.file.Path;
import java.util.List;

/**
 * Representerar en post eller handling som ska paketeras i ett E-ARK SIP.
 * Implementeringar tillhandahåller metadata-XML, metadata-typ och eventuella innehållsfiler.
 *
 * <p>Användning:</p>
 * <pre>{@code
 * ArchivableRecord post = ArchivableRecord.builder()
 *     .id("arende-2024-001")
 *     .metadataFile(metadataXmlPath)
 *     .metadataType("arende")
 *     .file(new SipFile("ansökan.pdf", stream))
 *     .build();
 *
 * Path zip = new RecordSipBuilder(sipPackager).build(post, workDir);
 * }</pre>
 */
public interface ArchivableRecord {

    /** Unikt ID — används som SIP-identifierare i METS. */
    String getId();

    /** Sökväg till den förrenderade metadata-XML-filen. */
    Path getMetadataFile();

    /** Metadata-typ, t.ex. "arende", "handling". Matchar metadataType i schema.yaml. */
    String getMetadataType();

    /** Innehållsfiler att inkludera i SIP-reprentationen. Kan vara tom. */
    List<SipFile> getFiles();

    static Builder builder() {
        return new Builder();
    }

    class Builder {
        private String id;
        private Path metadataFile;
        private String metadataType;
        private final java.util.ArrayList<SipFile> files = new java.util.ArrayList<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder metadataFile(Path metadataFile) {
            this.metadataFile = metadataFile;
            return this;
        }

        public Builder metadataType(String metadataType) {
            this.metadataType = metadataType;
            return this;
        }

        public Builder file(SipFile file) {
            this.files.add(file);
            return this;
        }

        public Builder files(List<SipFile> files) {
            this.files.addAll(files);
            return this;
        }

        public ArchivableRecord build() {
            if (id == null) throw new IllegalStateException("id krävs");
            if (metadataFile == null) throw new IllegalStateException("metadataFile krävs");
            if (metadataType == null) throw new IllegalStateException("metadataType krävs");
            var snapshot = List.copyOf(files);
            return new ArchivableRecord() {
                public String getId() { return id; }
                public Path getMetadataFile() { return metadataFile; }
                public String getMetadataType() { return metadataType; }
                public List<SipFile> getFiles() { return snapshot; }
            };
        }
    }
}
