package se.eterna.commons.sip;

import se.eterna.commons.client.TransferResource;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Abstraktion för vad som händer med ett färdigbyggt SIP ZIP.
 * Implementeringar kan spara till fil, ladda upp till ETERNA eller streama vidare.
 */
public interface SipOutputTarget {

    /**
     * Hanterar ett färdigbyggt SIP ZIP.
     *
     * @param filename  Filnamn att använda vid upload
     * @param zipPath   Sökväg till den byggda ZIP-filen
     * @return TransferResource om uploaden lyckas, null om målet är lokal fil
     */
    TransferResource complete(String filename, Path zipPath) throws IOException;
}
