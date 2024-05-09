package ch.obya.pta.common.util.validation;

public interface Checker<T> {
    T check(T input);
}