package edu.asu.jmars.parsers.gis;

import edu.asu.jmars.parsers.ParseException;
import edu.asu.jmars.parsers.ParseResult;
import edu.asu.jmars.parsers.Parser;
import edu.asu.jmars.parsers.simple.CharacterParser;

import java.util.Arrays;
import java.util.List;

public class DirectionParser extends Parser<Character> {
    private final Parser<java.lang.Character> parser;

    public DirectionParser() {
        List<Parser<Character>> directions = Arrays.asList(
                new CharacterParser('N'), new CharacterParser('n'), new CharacterParser('E'), new CharacterParser('e'),
                new CharacterParser('S'), new CharacterParser('s'), new CharacterParser('W'), new CharacterParser('w')
        );

        parser = oneOf(directions);
    }

    @Override
    public ParseResult<Character> parse(String s) throws ParseException {
        return this.parser.parse(s);
    }
}
