package edu.asu.jmars.parsers.gis;

import edu.asu.jmars.parsers.ParseException;
import edu.asu.jmars.parsers.ParseResult;
import edu.asu.jmars.parsers.Parser;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class DMSParser extends Parser<List<Double>> {
    private final Parser<List<Double>> parser;

    public DMSParser(Parser<Double> integer, Parser<Double> decimal, Parser<Character> degreeSymbol, Parser<Character> minuteSymbol, Parser<Character> secondSymbol) {
        Parser<Double> degrees = integer.stripLeadingWhitespace()
                .then(degreeSymbol)
                .map(Pair::getLeft)
                .stripLeadingWhitespace();

        Parser<Double> minutes = integer.stripLeadingWhitespace()
                .then(minuteSymbol)
                .map(Pair::getLeft)
                .stripLeadingWhitespace();

        Parser<Double> seconds = decimal.stripLeadingWhitespace()
                .then(secondSymbol)
                .map(Pair::getLeft);

        this.parser = seq(Arrays.asList(degrees, minutes, seconds)).stripLeadingWhitespace();
    }

    @Override
    public ParseResult<List<Double>> parse(String s) throws ParseException {
        return parser.parse(s);
    }
}
