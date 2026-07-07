package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.Setter;

@Setter
public class EventDateTimeDTO {

    private String standardDateTime;
    private String value;

    @XmlAttribute(name = "standarddatetime")
    public String getStandardDateTime() {
        return standardDateTime;
    }

    @XmlValue
    public String getValue() {
        return value;
    }
}
