package ru.ifmo.rain.alekperov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {

    private static final Comparator<Student> NAME_COMPARATOR = Comparator.comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparingInt(Student::getId);

    private static final String NAME_JOINER = " ";
    private static final String EMPTY = "";

    private static String getStudentFullName(final Student student) {
        return student.getFirstName().concat(NAME_JOINER).concat(student.getLastName());
    }

    private static Stream<Group> getGroupStream(final Collection<Student> collection,
                                                final Comparator<? super Student> studentOrder) {
        return collection.stream().collect(Collectors.groupingBy(Student::getGroup, Collectors.toCollection(() ->
                new TreeSet<>(studentOrder)))).entrySet().stream().map(e -> new Group(e.getKey(),
                new ArrayList<Student>(e.getValue())));
    }

    private static Stream<Group> getSortedGroupStream(final Collection<Student> collection,
                                                      final Comparator<? super Student> studentOrder,
                                                      final Comparator<? super Group> groupOrder) {
        return getGroupStream(collection, studentOrder).sorted(groupOrder);
    }

    private static Optional<Group> getMaxGroup(final Collection<Student> collection,
                                               final Comparator<? super Student> studentOrder,
                                               final Comparator<? super Group> groupOrder) {
        return getGroupStream(collection, studentOrder).max(groupOrder);
    }

    private static String getMaxGroupName(final Collection<Student> collection,
                                          final Comparator<? super Student> studentOrder,
                                          final Comparator<? super Group> groupOrder) {
        return getMaxGroup(collection, studentOrder, groupOrder).map(Group::getName).orElse(EMPTY);
    }

    private static <T, A, R> R mapStudentList(final List<Student> list,
                                                                 final Function<? super Student, ? extends T> mapper,
                                                                 final Collector<T, A, R> collector) {
        return list.stream().map(mapper).collect(collector);
    }

    private static <T> List<T> mapStudentList(final List<Student> list,
                                              final Function<? super Student, ? extends T> mapper) {
        return mapStudentList(list, mapper, Collectors.toList());
    }

    private static <T> Stream<Student> filterStudents(final Collection<Student> collection,
                                                      final Function<? super Student, ? extends T> getter,
                                                      final T expected) {
        return collection.stream().flatMap(student ->
                expected.equals(getter.apply(student)) ? Stream.of(student) : Stream.empty());
    }

    private static List<Student> getSortedStudentsList(final Stream<Student> stream,
                                                       final Comparator<? super Student> order) {
        return stream.sorted(order).collect(Collectors.toList());
    }

    private static List<Student> getSortedStudentsList(final Collection<Student> collection,
                                                       final Comparator<? super Student> order) {
        return getSortedStudentsList(collection.stream(), order);
    }

    private static <T> List<Student> listSortedByNameFilteredStudents(final Collection<Student> collection,
                                                                      final Function<? super Student, ? extends T> getter,
                                                                      final T expected) {
        return getSortedStudentsList(filterStudents(collection, getter, expected), NAME_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsByName(final Collection<Student> collection) {
        return getSortedGroupStream(collection, NAME_COMPARATOR, Comparator.comparing(Group::getName)).
                collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> collection) {
        return getSortedGroupStream(collection, Comparator.comparingInt(Student::getId),
                Comparator.comparing(Group::getName)).collect(Collectors.toList());
    }

    @Override
    public String getLargestGroup(final Collection<Student> collection) {
        return getMaxGroupName(collection, Comparator.naturalOrder(),
                Comparator.<Group>comparingInt(g -> g.getStudents().size()).
                        thenComparing(Group::getName, Comparator.reverseOrder()));
    }

    @Override
    public String getLargestGroupFirstName(final Collection<Student> collection) {
        return getMaxGroupName(collection, Comparator.naturalOrder(),
                Comparator.<Group>comparingInt(g -> getDistinctFirstNames(g.getStudents()).size()).
                        thenComparing(Group::getName, Comparator.reverseOrder()));
    }

    @Override
    public List<String> getFirstNames(final List<Student> list) {
        return mapStudentList(list, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> list) {
        return mapStudentList(list, Student::getLastName);
    }

    @Override
    public List<String> getGroups(final List<Student> list) {
        return mapStudentList(list, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final List<Student> list) {
        return mapStudentList(list, StudentDB::getStudentFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> list) {
        return mapStudentList(list, Student::getFirstName, Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(final List<Student> list) {
        return list.stream().sorted().map(Student::getFirstName).findFirst().orElse(EMPTY);
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> collection) {
        return getSortedStudentsList(collection, Comparator.naturalOrder());
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> collection) {
        return getSortedStudentsList(collection, NAME_COMPARATOR);
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> collection, final String name) {
        return listSortedByNameFilteredStudents(collection, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> collection, final String name) {
        return listSortedByNameFilteredStudents(collection, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> collection, final String group) {
        return listSortedByNameFilteredStudents(collection, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> collection, final String group) {
        return filterStudents(collection, Student::getGroup, group).collect(Collectors.toMap(Student::getLastName,
                Student::getFirstName, (n1, n2) -> Stream.of(n1, n2).min(Comparator.naturalOrder()).orElseThrow()));
    }

    @Override
    public String getMostPopularName(Collection<Student> collection) {
        return collection.stream().collect(Collectors.groupingBy(StudentDB::getStudentFullName,
                Collectors.mapping(Student::getGroup, Collectors.collectingAndThen(Collectors.toSet(), Set::size)))).
                entrySet().stream().max(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).
                thenComparing(Map.Entry::getKey)).map(Map.Entry::getKey).orElse(EMPTY);
    }
}
