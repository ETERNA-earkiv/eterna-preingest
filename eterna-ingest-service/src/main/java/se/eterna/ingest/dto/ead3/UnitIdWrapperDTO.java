package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Setter;

@Setter
public class UnitIdWrapperDTO {

    private String localType;
    private ReferenceDTO reference;

    @XmlAttribute(name = "localtype")
    public String getLocalType() {
        return localType;
    }

    @XmlElement(name = "ref")
    public ReferenceDTO getReference() {
        return reference;
    }
}
