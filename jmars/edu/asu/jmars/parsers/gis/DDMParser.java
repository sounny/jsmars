package edu.asu.jmars.parsers.gis;

import edu.asu.jmars.parsers.ParseException;
import edu.asu.jmars.parsers.ParseResult;
import edu.asu.jmars.parsers.Parser;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class DDMParser extends Parser<List<Double>> {
    private final Parser<List<Double>> parser;

    public DDMParser(Parser<Double> integer, Parser<Double> decimal, Parser<Character> degreeSymbol, Parser<Character> minuteSymbol) {
        Parser<Double> degrees = integer.stripLeadingWhitespace()
                .then(degreeSymbol)
                .map(Pair::getLeft)
                .stripLeadingWhitespace();

        Parser<Double> minutes = decimal.stripLeadingWhitespace()
                .then(minuteSymbol)
                .map(Pair::getLeft);

        this.parser = seq(Arrays.asList(degrees, minutes)).stripLeadingWhitespace();
    }

    @Override
    public ParseResult<List<Double>> parse(String s) throws ParseException {
        return this.parser.parse(s);
    }
}
