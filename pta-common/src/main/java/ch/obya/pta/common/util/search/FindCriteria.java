package ch.obya.pta.common.util.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record FindCriteria(String key, String operation, Object value) {

    public static Collection<FindCriteria> empty() {
        return Collections.emptyList();
    }

    public static Collection<FindCriteria> from(String filter) {
        List<FindCriteria> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("(\\w+?)(:|<|>)(\\w+?),");
        Matcher matcher = pattern.matcher(filter + ",");
        while (matcher.find()) {
            result.add(new FindCriteria(matcher.group(1), matcher.group(2), matcher.group(3)));
        }
        return result;
    }
}
