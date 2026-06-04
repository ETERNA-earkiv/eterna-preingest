package se.eterna.commons.client;

import org.springframework.core.io.InputStreamResource;

import java.io.InputStream;

/**
 * InputStreamResource med explicit filnamn — används vid multipart-upload till ETERNA.
 * Spring's standard InputStreamResource returnerar null från getFilename().
 */
public class NamedInputStreamResource extends InputStreamResource {

    private final String filename;

    public NamedInputStreamResource(String filename, InputStream inputStream) {
        super(inputStream);
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
