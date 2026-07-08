package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.Setter;

@Setter
public class ProcessingInformationDTO {

    private ParagraphDTO paragraph;

    @XmlElement(name = "p")
    public ParagraphDTO getParagraph() {
        return paragraph;
    }
}
