package ch.obya.pta.shopping.domain.vo;

import java.util.List;

import ch.obya.pta.shopping.domain.util.ArticleProblem;

import static ch.obya.pta.common.domain.util.CommonProblem.ifThrow;

/**
 * What a course (a session article) is about and how to take part.
 *
 * @param objectives        main objectives, in order
 * @param level             e.g. "All levels", "Intermediate"
 * @param prerequisites     e.g. minimum age, health notes
 * @param whatToBring       e.g. "Water and a mat"
 * @param cancellationHours a booked seat can be cancelled until this many hours before the start; absent: until the start
 */
public record CourseDetails(List<String> objectives, String level, String prerequisites, String whatToBring, Integer cancellationHours) {

    public CourseDetails {
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
        ifThrow(() -> cancellationHours != null && cancellationHours < 0,
                ArticleProblem.CancellationHoursInvalid.toException(cancellationHours));
    }
}
