package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.Setter;

@Setter
public class EventTypeDTO {

    private String value;

    @XmlAttribute(name = "value")
    public String getValue() {
        return value;
    }
}
