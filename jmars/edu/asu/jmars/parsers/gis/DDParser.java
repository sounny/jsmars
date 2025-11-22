package edu.asu.jmars.parsers.gis;

import edu.asu.jmars.parsers.ParseException;
import edu.asu.jmars.parsers.ParseResult;
import edu.asu.jmars.parsers.Parser;
import org.apache.commons.lang3.tuple.Pair;

public class DDParser extends Parser<Double> {
    private final Parser<Double> parser;

    public DDParser(Parser<Double> decimal, Parser<Character> degreeSymbol) {
        this.parser = decimal.stripLeadingWhitespace()
                .thenMaybe(degreeSymbol)
                .map(Pair::getLeft)
                .stripLeadingWhitespace();
    }

    @Override
    public ParseResult<Double> parse(String s) throws ParseException {
        return parser.parse(s);
    }
}
