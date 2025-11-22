package edu.asu.jmars.parsers.gis;

import edu.asu.jmars.parsers.ParseException;
import edu.asu.jmars.parsers.ParseResult;
import edu.asu.jmars.parsers.Parser;
import edu.asu.jmars.parsers.simple.CharacterParser;
import edu.asu.jmars.parsers.simple.DecimalParser;
import edu.asu.jmars.parsers.simple.IntegerParser;
import edu.asu.jmars.parsers.simple.SciDecimalParser;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import java.util.Arrays;
import java.util.List;

public class CoordinateParser extends Parser<Pair<Double, Character>> {
    private final Parser<? extends Pair<Double, Character>> parser;

    public CoordinateParser(Parser<Character> direction, char defaultDirection) {
        Parser<Double> integer = new IntegerParser();
        Parser<Double> decimal = oneOf(Arrays.asList(new SciDecimalParser(), new DecimalParser()));

        Parser<Character> degreeSymbol = oneOf(Arrays.asList(new CharacterParser('\u00B0'),
                new CharacterParser('d')));
        Parser<Character> minuteSymbol = oneOf(Arrays.asList(new CharacterParser('\''),
                new CharacterParser('\u2032')));
        Parser<Character> secondSymbol = new CharacterParser('"');

        Parser<Double> dms = new DMSParser(integer, decimal, degreeSymbol, minuteSymbol, secondSymbol)
                .map(this::convertDMStoDD);

        Parser<Double> ddm = new DDMParser(integer, decimal, degreeSymbol, minuteSymbol)
                .map(this::convertDDMtoDD);

        Parser<Double> dd = new DDParser(decimal, degreeSymbol);

        this.parser = oneOf(Arrays.asList(dms, ddm, dd))
                .thenMaybe(direction)
                .map(pair -> new ImmutablePair<>(pair.getLeft(), pair.getRight().orElse(defaultDirection)));
    }

    @Override
    public ParseResult<Pair<Double, Character>> parse(String s) throws ParseException {
        ParseResult<? extends Pair<Double, Character>> result = parser.parse(s);
        return new ParseResult<>(result.value, result.rest);
    }

    private Double convertDMStoDD(List<Double> values) throws IllegalArgumentException {
        if (values.size() != 3) {
            throw new IllegalArgumentException("DMS conversion requires 3 values");
        }

        return values.get(0) + values.get(1)/60 + values.get(2)/3600;
    }

    private Double convertDDMtoDD(List<Double> values) throws IllegalArgumentException {
        if (values.size() != 2) {
            throw new IllegalArgumentException("DDM conversion requires 2 values");
        }

        return values.get(0) + values.get(1)/60;
    }
}
