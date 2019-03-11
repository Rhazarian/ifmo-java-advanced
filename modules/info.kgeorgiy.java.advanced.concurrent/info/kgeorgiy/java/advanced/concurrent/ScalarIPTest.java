package info.kgeorgiy.java.advanced.concurrent;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.Collections;

/**
 * Basic tests for easy version
 * of <a href="https://www.kgeorgiy.info/courses/java-advanced/homeworks.html#homework-implementor">Implementor</a> homework
 * for <a href="https://www.kgeorgiy.info/courses/java-advanced/">Java Advanced</a> course.
 *
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScalarIPTest<P extends ScalarIP> extends BaseIPTest<P> {
    @Test
    public void test01_maximum() throws InterruptedException {
        test(Collections::max, ScalarIP::maximum, comparators);
    }

    @Test
    public void test02_minimum() throws InterruptedException {
        test(Collections::min, ScalarIP::minimum, comparators);
    }

    @Test
    public void test03_all() throws InterruptedException {
        test((data, predicate) -> data.stream().allMatch(predicate), ScalarIP::all, predicates);
    }

    @Test
    public void test04_any() throws InterruptedException {
        test((data, predicate) -> data.stream().anyMatch(predicate), ScalarIP::any, predicates);
    }
}
