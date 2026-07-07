package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.Setter;

@Setter
public class MaintenanceHistoryDTO {

    private MaintenanceEventDTO maintenanceEvent;

    @XmlElement(name = "maintenanceevent")
    public MaintenanceEventDTO getMaintenanceEvent() {
        return maintenanceEvent;
    }
}
