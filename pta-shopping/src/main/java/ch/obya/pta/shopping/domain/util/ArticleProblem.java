package ch.obya.pta.shopping.domain.util;


import ch.obya.pta.common.domain.util.Problem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ArticleProblem implements Problem {

    NoCustomer("Article %s does not exist."),
    AlreadyExisting("Article (%s) already exists in the period (%s,%s).");

    private final String text;
}
