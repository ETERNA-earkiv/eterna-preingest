package se.eterna.ingest.service;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import se.eterna.ingest.config.SchemaDefinition;
import se.eterna.ingest.dto.ead3.Ead3DTOWrapper;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Genererar metadata-XML från fältvärden och schema-definition.
 * Ingen template-fil behövs — XML-strukturen härledas direkt ur schemat.
 */
@Component
public class Ead3XmlGenerator {

    private static final Logger log = LoggerFactory.getLogger(Ead3XmlGenerator.class);

    public void generate(Ead3DTOWrapper ead3, Path xmlFilePath) throws JAXBException {
        try {
            var context = JAXBContext.newInstance(Ead3DTOWrapper.class);
            var marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            var schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
//            var schema = schemaFactory.newSchema(xsdFil);
//            marshaller.setSchema(schema);
            marshaller.marshal(ead3, xmlFilePath.toFile());
        } catch (JAXBException e) {
            log.error("Fel uppstod när XML skapades: {}", e.getMessage(), e);
            throw e;
        }
    }
}
