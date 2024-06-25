package ch.obya.pta.common.util.search;

import jakarta.annotation.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record AttributeFilter(Operator operator, Object value) {

    public interface Parser {
        Object parse(String value);
    }

    public static List<AttributeFilter> from(List<String> filters, Parser parser) {
        return filters.stream().map(filter -> from(filter, parser)).toList();
    }

    public static AttributeFilter from(String filter, Parser parser) {
        Pattern pattern = Pattern.compile("(eq:|ne:|lt:|gt:|le:|ge:|like:)*(.+)");
        Matcher matcher = pattern.matcher(filter);
        if (matcher.find()) {
            return matcher.groupCount() == 1 ?
                    new AttributeFilter(Operator.EQUALS, parser.parse(matcher.group(1))) :
                    new AttributeFilter(Operator.from(matcher.group(1)), parser.parse(matcher.group(2)));
        }
        throw new IllegalArgumentException("Invalid filter: " + filter);
    }

    public static AttributeFilter equal(Object value) {
        return new AttributeFilter(Operator.EQUALS, value);
    }

    public static AttributeFilter notEqual(Object value) {
        return new AttributeFilter(Operator.NOT_EQUALS, value);
    }

    public static AttributeFilter lessThan(Object value) {
        return new AttributeFilter(Operator.LESS_THAN, value);
    }

    public static AttributeFilter lessThanOrEqual(Object value) {
        return new AttributeFilter(Operator.LESS_THAN_EQUALS, value);
    }

    public static AttributeFilter greaterThan(Object value) {
        return new AttributeFilter(Operator.GREATER_THAN, value);
    }

    public static AttributeFilter greaterThanOrEqual(Object value) {
        return new AttributeFilter(Operator.GREATER_THAN_EQUALS, value);
    }

    public static AttributeFilter like(Object value) {
        return new AttributeFilter(Operator.LIKE, value);
    }

    public enum Operator {
        EQUALS("eq:"),
        NOT_EQUALS("ne:"),
        LESS_THAN("lt:"),
        LESS_THAN_EQUALS("le:"),
        GREATER_THAN("gt:"),
        GREATER_THAN_EQUALS("ge:"),
        LIKE("like:");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String symbol() {
            return symbol;
        }

        public static Operator from(@Nullable String symbol) {
            if (symbol == null) {
                return EQUALS;
            }
            for (Operator operator : Operator.values()) {
                if (operator.symbol().equals(symbol)) {
                    return operator;
                }
            }
            throw new IllegalArgumentException("Unknown filtering operation: " + symbol);
        }
    }
}
