package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.Setter;

@Setter
public class FileDescriptionDTO {

    private TitleStatementDTO titleStatement;

    @XmlElement(name = "titlestmt")
    public TitleStatementDTO getTitleStatement() {
        return titleStatement;
    }
}
