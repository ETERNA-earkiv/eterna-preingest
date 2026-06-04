package se.eterna.commons.client;

/**
 * Typsäkert resultat-wrapper för ETERNA API-anrop.
 * Anroparen kan använda pattern matching:
 *
 * <pre>{@code
 * switch (result) {
 *     case ApiSuccess<X> ok  -> process(ok.value());
 *     case ApiError<X>   err -> log.error(err.message());
 * }
 * }</pre>
 */
public sealed interface ApiResult<T> permits ApiSuccess, ApiError {

    default boolean isSuccess() {
        return this instanceof ApiSuccess<T>;
    }
}
