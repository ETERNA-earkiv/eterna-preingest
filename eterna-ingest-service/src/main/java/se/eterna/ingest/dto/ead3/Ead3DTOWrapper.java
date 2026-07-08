package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Setter;

@Setter
@XmlRootElement(name = "ead")
@XmlType(propOrder={"control", "archivalDescription"})
public class Ead3DTOWrapper {

    private ControlDTO control;
    private ArchivalDescriptionDTO archivalDescription;

    @XmlElement(name = "control")
    public ControlDTO getControl() {
        return control;
    }

    @XmlElement(name = "archdesc")
    public ArchivalDescriptionDTO getArchivalDescription() {
        return archivalDescription;
    }
}
