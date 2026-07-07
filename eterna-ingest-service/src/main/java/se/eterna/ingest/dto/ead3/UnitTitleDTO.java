package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.Setter;

@Setter
public class UnitTitleDTO {

    private String label;
    private String value;

    @XmlAttribute(name = "label")
    public String getLabel() {
        return label;
    }

    @XmlValue
    public String getValue() {
        return value;
    }

}
