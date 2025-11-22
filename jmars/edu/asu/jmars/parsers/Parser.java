package edu.asu.jmars.parsers;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class Parser<T> {
    public abstract ParseResult<T> parse(String s) throws ParseException;

    public final <U> Parser<U> map(Function<? super T, U> f) {
        return new Parser<U>() {
            @Override
            public ParseResult<U> parse(String s) throws ParseException {
                return Parser.this.parse(s).map(f);
            }
        };
    }

    public final Parser<T> done() {
        return new Parser<T>() {
            @Override
            public ParseResult<T> parse(String s) throws ParseException {
                ParseResult<T> result = Parser.this.parse(s);
                if (result.rest.isEmpty()) {
                    return result;
                }
                throw new ParseException("Unexpected input: " + result.rest);
            }
        };
    }

    public final <U> Parser<Pair<T, U>> then(Parser<U> p) {
        return new Parser<Pair<T, U>>() {
            @Override
            public ParseResult<Pair<T, U>> parse(String s) throws ParseException {
                ParseResult<T> r1 = Parser.this.parse(s);
                ParseResult<U> r2 = p.parse(r1.rest);

                return new ParseResult<>(new ImmutablePair<>(r1.value, r2.value), r2.rest);
            }
        };
    }

    public final <U> Parser<Pair<T, Optional<U>>> thenMaybe(Parser<U> p) {
        return new Parser<Pair<T, Optional<U>>>() {
            @Override
            public ParseResult<Pair<T, Optional<U>>> parse(String s) throws ParseException {
                ParseResult<T> r1 = Parser.this.parse(s);
                try {
                    ParseResult<U> r2 = p.parse(r1.rest);
                    return new ParseResult<>(new ImmutablePair<>(r1.value, Optional.of(r2.value)), r2.rest);
                } catch(ParseException ex) {
                    return new ParseResult<>(new ImmutablePair<>(r1.value, Optional.empty()), r1.rest);
                }
            }
        };
    }

    public final Parser<T> stripLeadingWhitespace() {
        return new Parser<T>() {
            @Override
            public ParseResult<T> parse(String s) throws ParseException {
                ParseResult<T> result = Parser.this.parse(s);
                return new ParseResult<>(result.value, StringUtils.stripStart(result.rest, " \t"));
            }
        };
    }

    public static <U> Parser<List<U>> seq(List<Parser<U>> parsers) {
        if (parsers.isEmpty()) {
            throw new IllegalArgumentException("Cannot seq empty list");
        }

        return new Parser<List<U>>() {
            @Override
            public ParseResult<List<U>> parse(String s) throws ParseException {
                ArrayList<U> results = new ArrayList<>(parsers.size());
                ParseResult<U> result = new ParseResult<>(null, s);

                for (Parser<U> parser : parsers) {
                    result = parser.parse(result.rest);
                    results.add(result.value);
                }

                return new ParseResult<>(results, result.rest);
            }
        };
    }

    public static <U> Parser<U> oneOf(List<Parser<U>> parsers) {
        if (parsers.isEmpty()) {
            throw new IllegalArgumentException("Cannot or empty list");
        }

        return new Parser<U>() {
            @Override
            public ParseResult<U> parse(String s) throws ParseException {
                String lastMessage = "";
                for (Parser<U> parser : parsers) {
                    try {
                        return parser.parse(s);
                    } catch (ParseException ex) {
                        lastMessage = ex.getMessage();
                    }
                }

                throw new ParseException(lastMessage);
            }
        };
    }

}
