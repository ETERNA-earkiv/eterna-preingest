package se.eterna.commons.sip;

import se.eterna.commons.client.TransferResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * SipOutputTarget som kopierar den färdiga SIP-ZIPen till en angiven katalog.
 * Användbart för testning, debug och integrationer som hämtar ZIP-filen extern.
 */
public class FileOutputTarget implements SipOutputTarget {

    private final Path outputDir;

    public FileOutputTarget(Path outputDir) {
        this.outputDir = outputDir;
    }

    @Override
    public TransferResource complete(String filename, Path zipPath) throws IOException {
        Files.createDirectories(outputDir);
        Path dest = outputDir.resolve(filename);
        Files.copy(zipPath, dest, StandardCopyOption.REPLACE_EXISTING);
        return null;
    }
}
