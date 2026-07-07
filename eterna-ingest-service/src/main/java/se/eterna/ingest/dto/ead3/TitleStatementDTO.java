package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.Setter;

@Setter
public class TitleStatementDTO {

    private String titleProper;

    @XmlElement(name = "titleproper")
    public String getTitleProper() {
        return titleProper;
    }
}
