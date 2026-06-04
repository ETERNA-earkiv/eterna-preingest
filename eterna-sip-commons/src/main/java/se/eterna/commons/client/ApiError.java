package se.eterna.commons.client;

/**
 * Representerar ett API-fel med feltyp och meddelande.
 *
 * @param type    HTTP-felkod eller domänfelkod, t.ex. "NOT_FOUND", "UNAUTHORIZED"
 * @param message Läsbart felmeddelande från ETERNA
 */
public record ApiError<T>(String type, String message) implements ApiResult<T> {}
