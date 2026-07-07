package se.eterna.ingest.service;

import org.springframework.stereotype.Service;
import se.eterna.ingest.dto.ead3.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

@Service
public class Ead3Service {

    public record ArchivalDescriptionParameters(String level,
                                                String system,
                                                String systemId,
                                                String relatedMaterial,
                                                String securityClass,
                                                String classificationStructure,
                                                String classificationStructureVersion,
                                                String structuralUnit,
                                                String structuralUnitDenotation,
                                                String processInfo) {}

    public Ead3DTOWrapper createEad3(String title,
                                     String recordId,
                                     String agencyName,
                                     ArchivalDescriptionParameters archivalDescriptionParameters) {
        var ead = new Ead3DTOWrapper();

        // control
        var control = createControl(recordId, title, agencyName);
        ead.setControl(control);

        // archdesc
        var archivalDescription = createArchivalDescription(title, archivalDescriptionParameters);

        ead.setArchivalDescription(archivalDescription);

        return ead;
    }

    private ControlDTO createControl(String recordId, String titleProper, String agencyName) {
        var control = new ControlDTO();

        // recordid
        control.setRecordId(recordId);

        // filedesc
        // titlestmt
        // titleproper
        var titleStatement = new TitleStatementDTO();
        titleStatement.setTitleProper(titleProper);
        var fileDescription = new FileDescriptionDTO();
        fileDescription.setTitleStatement(titleStatement);
        control.setFileDescription(fileDescription);

        // maintenancestatus
        var maintenanceStatus = new MaintenanceStatusDTO();
        maintenanceStatus.setValue("new");
        control.setMaintenanceStatus(maintenanceStatus);

        // maintenanceagency
        var maintenanceAgency = new MaintenanceAgencyDTO();
        maintenanceAgency.setAgencyName(agencyName);
        control.setMaintenanceAgency(maintenanceAgency);

        // eventtype
        var eventType = new EventTypeDTO();
        eventType.setValue("created");

        // eventdatetime
        var eventDateTime = new EventDateTimeDTO();
        var dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        var creationDate = new Date();
        var creationDateAsString = dateFormat.format(creationDate);
        eventDateTime.setStandardDateTime(creationDateAsString);
        eventDateTime.setValue(creationDateAsString);

        // agenttype
        var agentType = new AgentTypeDTO();
        agentType.setValue("machine");

        // agent
        var agent = "ETERNA Preingest";

        // maintenanceevent
        var maintenanceEvent = new MaintenanceEventDTO();
        maintenanceEvent.setEventType(eventType);
        maintenanceEvent.setEventDateTime(eventDateTime);
        maintenanceEvent.setAgentType(agentType);
        maintenanceEvent.setAgent(agent);

        // maintenancehistory
        var maintenanceHistory = new MaintenanceHistoryDTO();
        maintenanceHistory.setMaintenanceEvent(maintenanceEvent);
        control.setMaintenanceHistory(maintenanceHistory);

        return control;
    }

    private ArchivalDescriptionDTO createArchivalDescription(String title, ArchivalDescriptionParameters archivalDescriptionParameters) {

        // archdesc
        var archivalDescription = new ArchivalDescriptionDTO();
        archivalDescription.setLevel(archivalDescriptionParameters.level);

        // did
        var descriptiveIdentification = new DescriptiveIdentificationDTO();

        // unittitle
        var unitTitle = new UnitTitleDTO();
        unitTitle.setValue(title);
        descriptiveIdentification.setUnitTitle(unitTitle);

        // ref
        var reference = new ReferenceDTO();
        reference.setId(archivalDescriptionParameters.systemId);
        reference.setValue(archivalDescriptionParameters.system);

        // unitid
        var unitIdWrapper = new UnitIdWrapperDTO();
        unitIdWrapper.setReference(reference);
        unitIdWrapper.setLocalType("databasecompilation");
        descriptiveIdentification.setUnitIdWrapper(unitIdWrapper);

        // didnote
        var descriptiveIdentificationNote = new DescriptiveIdentificationNoteDTO();
        descriptiveIdentificationNote.setLocalType("securityclass");
        descriptiveIdentificationNote.setValue(archivalDescriptionParameters.securityClass);
        descriptiveIdentification.setDescriptiveIdentificationNote(descriptiveIdentificationNote);

        archivalDescription.setDescriptiveIdentification(descriptiveIdentification);

        // dsc
        var subordinateComponentsDescription = new SubordinateComponentsDescriptionDTO();

        // c
        var componentlista = new ArrayList<ComponentDTO>();
        var component = new ComponentDTO();
        component.setLevel("otherlevel");
        component.setOtherLevel("Classificationstructure");
        descriptiveIdentification = new DescriptiveIdentificationDTO();
        unitTitle = new UnitTitleDTO();
        unitTitle.setLabel("Klassificeringsstruktur");
        unitTitle.setValue(archivalDescriptionParameters.classificationStructure);
        descriptiveIdentification.setUnitTitle(unitTitle);
        var unitId = new UnitIdDTO();
        unitId.setLabel("Version");
        unitId.setValue(archivalDescriptionParameters.classificationStructureVersion);
        descriptiveIdentification.setUnitId(unitId);
        component.setDescriptiveIdentification(descriptiveIdentification);
        componentlista.add(component);

        // c
        component = new ComponentDTO();
        component.setLevel("otherlevel");
        component.setOtherLevel("Structuralunit");
        descriptiveIdentification = new DescriptiveIdentificationDTO();
        unitTitle = new UnitTitleDTO();
        unitTitle.setLabel("Strukturenhet");
        unitTitle.setValue(archivalDescriptionParameters.structuralUnit);
        descriptiveIdentification.setUnitTitle(unitTitle);
        unitId = new UnitIdDTO();
        unitId.setLabel("Beteckning");
        unitId.setValue(archivalDescriptionParameters.structuralUnitDenotation);
        descriptiveIdentification.setUnitId(unitId);
        component.setDescriptiveIdentification(descriptiveIdentification);
        componentlista.add(component);

        subordinateComponentsDescription.setComponent(componentlista);
        archivalDescription.setSubordinateComponentsDescription(subordinateComponentsDescription);

        // relatedmaterial
        var paragraph = new ParagraphDTO();
        paragraph.setValue(archivalDescriptionParameters.relatedMaterial);
        var relatedMaterialDTO = new RelatedMaterialDTO();
        relatedMaterialDTO.setParagraph(paragraph);

        archivalDescription.setRelatedMaterial(relatedMaterialDTO);

        // processinfo
        var processingInformation = new ProcessingInformationDTO();
        paragraph = new ParagraphDTO();
        paragraph.setValue(archivalDescriptionParameters.processInfo);
        processingInformation.setParagraph(paragraph);

        archivalDescription.setProcessingInformation(processingInformation);

        return archivalDescription;
    }
}
