package se.eterna.commons.client;

public record ApiSuccess<T>(T value) implements ApiResult<T> {}
