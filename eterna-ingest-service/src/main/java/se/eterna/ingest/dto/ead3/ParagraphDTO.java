package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlValue;
import lombok.Setter;

@Setter
public class ParagraphDTO {

    private String value;

    @XmlValue
    public String getValue() {
        return value;
    }
}
