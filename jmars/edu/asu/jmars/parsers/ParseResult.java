package edu.asu.jmars.parsers;

import java.util.function.Function;

public final class ParseResult<T> {
    public final T value;
    public final String rest;

    public ParseResult(T value, String rest) {
        this.value = value;
        this.rest = rest;
    }

    public <U> ParseResult<U> map(Function<? super T, U> f) {
        return new ParseResult<>(f.apply(this.value), this.rest);
    }

    public String toString() {
        return "Value: " + value + " Rest: " + rest;
    }
}
