package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.Setter;

import java.util.List;

@Setter
public class SubordinateComponentsDescriptionDTO {

    private List<ComponentDTO> component;

    @XmlElement(name = "c")
    public List<ComponentDTO> getComponent() {
        return component;
    }
}
