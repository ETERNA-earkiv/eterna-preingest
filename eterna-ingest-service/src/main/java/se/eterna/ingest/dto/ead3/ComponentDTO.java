package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Setter;

@Setter
public class ComponentDTO {

    private String level;
    private String otherLevel;
    private DescriptiveIdentificationDTO descriptiveIdentification;

    @XmlAttribute(name = "level")
    public String getLevel() {
        return level;
    }

    @XmlAttribute(name = "otherlevel")
    public String getOtherLevel() {
        return otherLevel;
    }

    @XmlElement(name = "did")
    public DescriptiveIdentificationDTO getDescriptiveIdentification() {
        return descriptiveIdentification;
    }
}
