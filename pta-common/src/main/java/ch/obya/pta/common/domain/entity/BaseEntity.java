package ch.obya.pta.common.domain.entity;

import java.util.ArrayList;
import java.util.List;

import ch.obya.pta.common.domain.event.*;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.domain.vo.Identity;
import lombok.*;
import lombok.experimental.Accessors;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

@ToString(of = {"id", "state"}, doNotUseGetters = true)
@EqualsAndHashCode(of = "id", callSuper = false)
@Accessors(chain = true, fluent = true)
public abstract class BaseEntity<E extends BaseEntity<E, I, S>, I extends Identity, S> implements Entity<E, I, S> {
    @Getter
    protected I id;
    protected S state;
    private final List<Event> events = new ArrayList<>();

    protected BaseEntity(I id, S state) {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("Entity.id"));
        ifNullThrow(state, CommonProblem.AttributeNotNull.toException("Entity.state"));
        this.id = id;
        this.state = state;
    }

    @Override
    public S state() {
        return cloneState();
    }

    @Override
    public List<Event> domainEvents() {
        var result = List.copyOf(events);
        events.clear();
        return result;
    }

    protected S validate(S state) {
        return state;
    }

    protected E andEvent(Event...e) {
        events.addAll(List.of(e));
        return (E) this;
    }

    protected abstract S cloneState();
}
