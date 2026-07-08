package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.Setter;

@Setter
public class ReferenceDTO {

    private String id;
    private String value;

    @XmlAttribute(name = "id")
    public String getId() {
        return id;
    }

    @XmlValue
    public String getValue() {
        return value;
    }
}
