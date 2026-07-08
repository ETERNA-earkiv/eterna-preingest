package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Setter;

@Setter
@XmlType(propOrder={"recordId", "fileDescription", "maintenanceStatus", "maintenanceAgency", "maintenanceHistory"})
public class ControlDTO {

    private String recordId;
    private FileDescriptionDTO fileDescription;
    private MaintenanceStatusDTO maintenanceStatus;
    private MaintenanceAgencyDTO maintenanceAgency;
    private MaintenanceHistoryDTO maintenanceHistory;

    @XmlElement(name = "recordid")
    public String getRecordId() {
        return recordId;
    }

    @XmlElement(name = "filedesc")
    public FileDescriptionDTO getFileDescription() {
        return fileDescription;
    }

    @XmlElement(name = "maintenancestatus")
    public MaintenanceStatusDTO getMaintenanceStatus() {
        return maintenanceStatus;
    }

    @XmlElement(name = "maintenanceagency")
    public MaintenanceAgencyDTO getMaintenanceAgency() {
        return maintenanceAgency;
    }

    @XmlElement(name = "maintenancehistory")
    public MaintenanceHistoryDTO getMaintenanceHistory() {
        return maintenanceHistory;
    }
}
