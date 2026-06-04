package se.eterna.commons.xml;

import se.eterna.commons.exception.XmlValidationException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stax.StAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Streaming XML-validering mot ett {@link Schema} utan att läsa in hela dokumentet i minnet.
 *
 * <p>Validering körs på en separat tråd medan data matas in via {@link #accept(ByteBuffer)}.
 * Anropa {@link #finish()} när all data är matad — det blockerar tills valideringen är klar
 * och kastar {@link XmlValidationException} om dokumentet inte uppfyller schemat.</p>
 *
 * <pre>{@code
 * StaxXmlValidator validator = new StaxXmlValidator(schema);
 * validator.start();
 * for (ByteBuffer chunk : xmlChunks) {
 *     validator.accept(chunk);
 * }
 * validator.finish(); // kastar XmlValidationException om ogiltig XML
 * }</pre>
 */
public final class StaxXmlValidator {

    private final XmlValidatorInputStream input = new XmlValidatorInputStream();
    private volatile Thread validationThread;
    private volatile XmlValidationException validationError;
    private volatile boolean finished;

    /**
     * Startar valideringstråden. Måste anropas innan {@link #accept}.
     */
    public void start(Schema schema) {
        validationThread = Thread.ofVirtual().start(() -> runValidation(schema));
    }

    private void runValidation(Schema schema) {
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Validator validator = schema.newValidator();
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            validator.validate(new StAXSource(reader));
        } catch (Exception e) {
            input.fail(new IOException("XML-validering misslyckades", e));
            validationError = new XmlValidationException(e);
        }
    }

    /**
     * Mata in nästa datachunk. Kasta aldrig efter {@link #finish}.
     */
    public void accept(ByteBuffer buffer) {
        if (finished) throw new IllegalStateException("Validatorn är redan avslutad");
        input.feed(buffer);
    }

    /**
     * Signalerar att all data är matad och väntar tills valideringen är klar.
     *
     * @throws XmlValidationException om XML-dokumentet är ogiltigt
     */
    public void finish() {
        finished = true;
        input.closeInput();
        if (validationThread != null) {
            try {
                validationThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (validationError != null) {
            throw validationError;
        }
    }
}
