package se.eterna.ingest.dto.ead3;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Setter;

@Setter
@XmlType(propOrder={"eventType", "eventDateTime", "agentType", "agent"})
public class MaintenanceEventDTO {

    private EventTypeDTO eventType;
    private EventDateTimeDTO eventDateTime;
    private AgentTypeDTO agentType;
    private String agent;

    @XmlElement(name = "eventtype")
    public EventTypeDTO getEventType() {
        return eventType;
    }

    @XmlElement(name = "eventdatetime")
    public EventDateTimeDTO getEventDateTime() {
        return eventDateTime;
    }

    @XmlElement(name = "agenttype")
    public AgentTypeDTO getAgentType() {
        return agentType;
    }

    @XmlElement(name = "agent")
    public String getAgent() {
        return agent;
    }
}
