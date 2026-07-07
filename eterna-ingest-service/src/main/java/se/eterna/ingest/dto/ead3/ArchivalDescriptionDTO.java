package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Setter;

@Setter
public class ArchivalDescriptionDTO {

    private String level;
    private DescriptiveIdentificationDTO descriptiveIdentification;
    private SubordinateComponentsDescriptionDTO subordinateComponentsDescription;
    private RelatedMaterialDTO relatedMaterial;
    private ProcessingInformationDTO processingInformation;

    @XmlAttribute(name = "level")
    public String getLevel() {
        return level;
    }

    @XmlElement(name = "did")
    public DescriptiveIdentificationDTO getDescriptiveIdentification() {
        return descriptiveIdentification;
    }

    @XmlElement(name = "dsc")
    public SubordinateComponentsDescriptionDTO getSubordinateComponentsDescription() {
        return subordinateComponentsDescription;
    }

    @XmlElement(name = "relatedmaterial")
    public RelatedMaterialDTO getRelatedMaterial() {
        return relatedMaterial;
    }

    @XmlElement(name = "processinfo")
    public ProcessingInformationDTO getProcessingInformation() {
        return processingInformation;
    }
}
