package se.eterna.commons.sip;

import org.roda_project.commons_ip2.model.IP;
import org.roda_project.commons_ip2.model.IPDescriptiveMetadata;
import org.roda_project.commons_ip2.model.IPFile;
import org.roda_project.commons_ip2.model.IPRepresentation;
import org.roda_project.commons_ip2.model.MetadataType;
import org.roda_project.commons_ip2.model.SIP;
import org.roda_project.commons_ip2.model.impl.eark.EARKSIP;
import org.roda_project.commons_ip2.utils.SIPBuilderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SipPackager {

    private static final Logger log = LoggerFactory.getLogger(SipPackager.class);
    private static final String CREATOR_AGENT = "eterna-ingest-service";
    private static final String CREATOR_VERSION = "1.0.0";
    private static final String REPRESENTATION_ID = "rep1";

    /**
     * Bygger ett E-ARK SIP ZIP från metadata-XML och filer.
     * Returnerar en temporär Path till ZIP-filen — anroparen ansvarar för cleanup.
     */
    public Path buildZip(
        String sipId,
        Path metadataFile,
        String metadataType,
        List<SipFile> files,
        Path workDir
    ) throws Exception {
        SIP sip = new EARKSIP(sipId, IP.SIP_TYPE_EARK2, null, null, null);
        sip.addCreatorSoftwareAgent(CREATOR_AGENT, CREATOR_VERSION);

        // Beskrivande metadata
        MetadataType mdType = new MetadataType(MetadataType.MetadataTypeEnum.OTHER);
        mdType.setOtherType(metadataType);
        sip.addDescriptiveMetadata(new IPDescriptiveMetadata(
            new IPFile(metadataFile), mdType, null
        ));

        // Filer i representation
        if (!files.isEmpty()) {
            IPRepresentation representation = new IPRepresentation(REPRESENTATION_ID);
            for (SipFile sipFile : files) {
                Path tempFile = copyToTemp(sipFile, workDir);
                representation.addFile(new IPFile(tempFile));
            }
            sip.addRepresentation(representation);
        }

        return sip.build(SIPBuilderUtils.getWriteStrategy(
            SIPBuilderUtils.WriteStrategyEnum.ZIP, workDir
        ));
    }

    private Path copyToTemp(SipFile sipFile, Path workDir) throws IOException {
        Path dest = workDir.resolve(sipFile.filename());
        try (InputStream in = sipFile.content()) {
            Files.copy(in, dest);
        }
        return dest;
    }
}
