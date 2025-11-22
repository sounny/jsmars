package edu.asu.jmars.parsers.simple;

import edu.asu.jmars.parsers.ParseException;
import edu.asu.jmars.parsers.Parser;
import edu.asu.jmars.parsers.ParseResult;

public class CharacterParser extends Parser<Character> {
    private final char value;
    private final String text;

    public CharacterParser(char value) {
        this.value = value;
        this.text = String.valueOf(value);
    }

    public CharacterParser(char value, String text) {
        this.value = value;
        this.text = text;
    }

    @Override
    public ParseResult<Character> parse(String s) throws ParseException {
        if (s.isEmpty()) {
            throw new ParseException("Cannot parse empty string");
        }

        char c = s.charAt(0);
        if (c != value) {
            throw new ParseException(String.format("Actual: %c, Expected: %s", c, text));
        }

        return new ParseResult<>(c, s.substring(1));
    }
}
