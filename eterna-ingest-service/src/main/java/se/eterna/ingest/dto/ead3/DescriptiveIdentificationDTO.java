package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Setter;

@Setter
@XmlType(propOrder={"unitTitle", "unitId", "unitIdWrapper", "descriptiveIdentificationNote"})
public class DescriptiveIdentificationDTO {

    private UnitTitleDTO unitTitle;
    private UnitIdDTO unitId;
    private UnitIdWrapperDTO unitIdWrapper;
    private DescriptiveIdentificationNoteDTO descriptiveIdentificationNote;

    @XmlElement(name = "unittitle")
    public UnitTitleDTO getUnitTitle() {
        return unitTitle;
    }

    @XmlElement(name = "unitid")
    public UnitIdDTO getUnitId() {
        return unitId;
    }

    @XmlElement(name = "unitid")
    public UnitIdWrapperDTO getUnitIdWrapper() {
        return unitIdWrapper;
    }

    @XmlElement(name = "didnote")
    public DescriptiveIdentificationNoteDTO getDescriptiveIdentificationNote() {
        return descriptiveIdentificationNote;
    }
}
