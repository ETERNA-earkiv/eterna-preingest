package se.eterna.commons.sip;

import se.eterna.commons.client.EternaClient;
import se.eterna.commons.client.TransferResource;

import java.io.IOException;
import java.nio.file.Path;

/**
 * SipOutputTarget som laddar upp SIP-ZIPen direkt till ETERNA via {@link EternaClient}.
 */
public class EternaUploadTarget implements SipOutputTarget {

    private final EternaClient client;

    public EternaUploadTarget(EternaClient client) {
        this.client = client;
    }

    @Override
    public TransferResource complete(String filename, Path zipPath) throws IOException {
        return client.uploadZip(filename, zipPath);
    }
}
