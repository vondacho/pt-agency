package ch.obya.pta.common.util;

import ch.obya.pta.common.util.search.FindCriteria;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FindCriteriaTest {

    @Test
    void should_build_criteria_from_filter() {
        assertThat(FindCriteria.from("firstName:albert"))
                .containsExactly(new FindCriteria("firstName", ":", "albert"));

        assertThat(FindCriteria.from("firstName:albert,min>0,max<10"))
                .containsExactlyInAnyOrder(
                        new FindCriteria("firstName", ":", "albert"),
                        new FindCriteria("min", ">", "0"),
                        new FindCriteria("max", "<", "10"));
    }
}
