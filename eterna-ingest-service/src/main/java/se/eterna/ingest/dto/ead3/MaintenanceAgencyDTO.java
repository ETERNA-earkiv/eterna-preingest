package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.Setter;

@Setter
public class MaintenanceAgencyDTO {

    private String agencyName;

    @XmlElement(name = "agencyname")
    public String getAgencyName() {
        return agencyName;
    }
}
