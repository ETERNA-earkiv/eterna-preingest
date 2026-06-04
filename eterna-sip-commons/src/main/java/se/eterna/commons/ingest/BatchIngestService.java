package se.eterna.commons.ingest;

import java.nio.file.Path;
import java.util.List;

/**
 * Batch-ingest av flera SIP-paket mot ETERNA.
 * Laddar upp alla paket, startar ingest-jobb och väntar på att alla ska bli klara.
 */
public interface BatchIngestService {

    /**
     * Paket för ett enskilt batch-ingest-jobb.
     *
     * @param filename Filnamn för SIP-ZIPen
     * @param zipPath  Sökväg till SIP-ZIPen
     * @param options  Ingest-alternativ (parentAipId etc.)
     */
    record SipJob(String filename, Path zipPath, IngestOptions options) {}

    /**
     * Ingestas alla paket sekventiellt. Fortsätter vid fel på enstaka jobb.
     *
     * @param jobs Lista med SIP-paket
     * @return Resultat per jobb, i samma ordning som indata
     */
    List<IngestResult> ingestAll(List<SipJob> jobs);
}
