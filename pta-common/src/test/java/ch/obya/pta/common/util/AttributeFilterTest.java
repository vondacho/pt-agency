package ch.obya.pta.common.util;

import ch.obya.pta.common.util.search.AttributeFilter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeFilterTest {

    @Test
    void should_build_attribute_filter() {
        assertThat(AttributeFilter.from("eq:albert", s -> s)).isEqualTo(AttributeFilter.equal("albert"));
        assertThat(AttributeFilter.from("eq:1958-12-12", s -> s)).isEqualTo(AttributeFilter.equal("1958-12-12"));
    }
}
