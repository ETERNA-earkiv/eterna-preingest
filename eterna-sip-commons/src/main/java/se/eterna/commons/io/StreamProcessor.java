package se.eterna.commons.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Composable OutputStream-dekoratör. Implementeringar kan lägga till checksumberäkning,
 * ZIP-entry-hantering, kryptering eller annan behandling i en pipeline.
 */
@FunctionalInterface
public interface StreamProcessor {

    OutputStream wrap(OutputStream out) throws IOException;

    /** Komponera: kör this.wrap() och sedan next.wrap() på resultatet. */
    default StreamProcessor then(StreamProcessor next) {
        return out -> next.wrap(this.wrap(out));
    }
}
