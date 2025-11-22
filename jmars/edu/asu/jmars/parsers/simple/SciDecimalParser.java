package edu.asu.jmars.parsers.simple;

import edu.asu.jmars.parsers.ParseException;
import edu.asu.jmars.parsers.ParseResult;
import edu.asu.jmars.parsers.Parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SciDecimalParser extends Parser<Double> {
    @Override
    public ParseResult<Double> parse(String s) throws ParseException {
        if (s.isEmpty()) {
            throw new ParseException("Cannot parse empty string");
        }

        Pattern p = Pattern.compile("^([-+]?\\d+\\.?\\d*[eE]-?\\d+)(.*)");
        Matcher m = p.matcher(s);
        if (!m.find()) {
            throw new ParseException("Could not match scientific decimal");
        }

        return new ParseResult<>(Double.valueOf(m.group(1)), m.group(2));
    }
}
