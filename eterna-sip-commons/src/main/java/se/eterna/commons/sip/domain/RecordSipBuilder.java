package se.eterna.commons.sip.domain;

import se.eterna.commons.exception.SipBuildException;
import se.eterna.commons.sip.SipPackager;

import java.nio.file.Path;

/**
 * Bygger ett E-ARK SIP ZIP från ett {@link ArchivableRecord}.
 * Delegerar SIP-paketeringen till {@link SipPackager} och erbjuder ett domänvänligt API.
 */
public class RecordSipBuilder {

    private final SipPackager packager;

    public RecordSipBuilder(SipPackager packager) {
        this.packager = packager;
    }

    /**
     * Paketerar posten som ett E-ARK SIP ZIP.
     *
     * @param record  Posten att paketera
     * @param workDir Temporär katalog för bygget
     * @return Sökväg till den byggda ZIP-filen
     */
    public Path build(ArchivableRecord record, Path workDir) {
        try {
            return packager.buildZip(
                record.getId(),
                record.getMetadataFile(),
                record.getMetadataType(),
                record.getFiles(),
                workDir
            );
        } catch (Exception e) {
            throw new SipBuildException(
                "Kunde inte paketera SIP för post " + record.getId(), e);
        }
    }
}
