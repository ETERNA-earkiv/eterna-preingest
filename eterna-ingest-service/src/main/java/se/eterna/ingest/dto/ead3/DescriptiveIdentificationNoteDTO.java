package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.Setter;

@Setter
public class DescriptiveIdentificationNoteDTO {

    private String localType;
    private String value;

    @XmlAttribute(name = "localtype")
    public String getLocalType() {
        return localType;
    }

    @XmlValue
    public String getValue() {
        return value;
    }
}
