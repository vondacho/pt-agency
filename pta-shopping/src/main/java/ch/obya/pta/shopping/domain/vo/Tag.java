package ch.obya.pta.shopping.domain.vo;

import java.util.Set;
import java.util.stream.Collectors;

public record Tag(String name) {

    public enum Default {
        SESSION, COURSE, PERSONAL_TRAINING,
        SUBSCRIPTION, PASS,
        ONE_YEAR, THREE_MONTHS, TWO_MONTHS, ONE_MONTH, ONE_WEEK, ONE_DAY, ONE_SESSION,
        DRINK, SNACK, MASS_GAINER,
        WELLNESS, SAUNA, HAMMAM, SWIMMING, MASSAGE,
        FREE, CHARGEABLE;

        public Tag toTag() {
            return from(this);
        }
    }

    public static Tag from(Default tag) {
        return new Tag(tag.name());
    }

    public static Set<Tag> fromMany(String tags) {
        return Set.of(tags.split(",")).stream()
                .map(Tag::new)
                .collect(Collectors.toSet());
    }
}


