package ru.ifmo.rain.alekperov;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {

    private static final Comparator<Student> NAME_COMPARATOR = Comparator.comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparingInt(Student::getId);

    private static Stream<Group> getGroupStream(Collection<Student> collection, Comparator<? super Student> studentOrder) {
        return collection.stream().collect(Collectors.groupingBy(Student::getGroup, Collectors.toCollection(() ->
                new TreeSet<>(studentOrder)))).entrySet().stream().map(e -> new Group(e.getKey(),
                new ArrayList<Student>(e.getValue())));
    }

    private static Stream<Group> getSortedGroupStream(Collection<Student> collection,
                                               Comparator<? super Student> studentOrder,
                                               Comparator<? super Group> groupOrder) {
        return getGroupStream(collection, studentOrder).sorted(groupOrder);
    }

    private static Optional<Group> getMaxGroup(Collection<Student> collection,
                                        Comparator<? super Student> studentOrder,
                                        Comparator<? super Group> groupOrder) {
        return getGroupStream(collection, studentOrder).max(groupOrder);
    }

    private static String getMaxGroupName(Collection<Student> collection,
                                   Comparator<? super Student> studentOrder,
                                   Comparator<? super Group> groupOrder) {
        return getMaxGroup(collection, studentOrder, groupOrder).map(Group::getName).orElse("");
    }

    private static <T, C extends Collection<T>> C mapStudentList(List<Student> list,
                                       Function<? super Student, ? extends T> mapper,
                                       Supplier<C> collectionSupplier) {
        return list.stream().map(mapper).collect(Collectors.toCollection(collectionSupplier));
    }

    private static <T> List<T> mapStudentList(List<Student> list, Function<? super Student, ? extends T> mapper) {
        return mapStudentList(list, mapper, ArrayList::new);
    }

    private static <T> Stream<Student> filterStudents(Collection<Student> collection,
                                                      Function<? super Student, ? extends T> getter, T expected) {
        return collection.stream().flatMap(student ->
                expected.equals(getter.apply(student)) ? Stream.of(student) : Stream.empty());
    }

    private static List<Student> getSortedStudentsList(Stream<Student> stream, Comparator<? super Student> order) {
        return stream.sorted(order).collect(Collectors.toList());
    }

    private static List<Student> getSortedStudentsList(Collection<Student> collection, Comparator<? super Student> order) {
        return getSortedStudentsList(collection.stream(), order);
    }

    private static <T> List<Student> listSortedByNameFilteredStudents(Collection<Student> collection,
                                                                      Function<? super Student, ? extends T> getter,
                                                                      T expected) {
        return getSortedStudentsList(filterStudents(collection, getter, expected), NAME_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> collection) {
        return getSortedGroupStream(collection, NAME_COMPARATOR, Comparator.comparing(Group::getName)).
                collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> collection) {
        return getSortedGroupStream(collection, Comparator.comparingInt(Student::getId),
                Comparator.comparing(Group::getName)).collect(Collectors.toList());
    }

    @Override
    public String getLargestGroup(Collection<Student> collection) {
        return getMaxGroupName(collection, Comparator.naturalOrder(),
                Comparator.<Group>comparingInt(g -> g.getStudents().size()).
                        thenComparing(Group::getName, Comparator.reverseOrder()));
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> collection) {
        return getMaxGroupName(collection, Comparator.naturalOrder(),
                Comparator.<Group>comparingInt(g -> getDistinctFirstNames(g.getStudents()).size()).
                        thenComparing(Group::getName, Comparator.reverseOrder()));
    }

    @Override
    public List<String> getFirstNames(List<Student> list) {
        return mapStudentList(list, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> list) {
        return mapStudentList(list, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> list) {
        return mapStudentList(list, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> list) {
        return mapStudentList(list, student -> student.getFirstName() + " " + student.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> list) {
        return mapStudentList(list, Student::getFirstName, TreeSet::new);
    }

    @Override
    public String getMinStudentFirstName(List<Student> list) {
        return list.stream().sorted().map(Student::getFirstName).findFirst().orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> collection) {
        return getSortedStudentsList(collection, Comparator.naturalOrder());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> collection) {
        return getSortedStudentsList(collection, NAME_COMPARATOR);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> collection, String s) {
        return listSortedByNameFilteredStudents(collection, Student::getFirstName, s);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> collection, String s) {
        return listSortedByNameFilteredStudents(collection, Student::getLastName, s);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> collection, String s) {
        return listSortedByNameFilteredStudents(collection, Student::getGroup, s);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> collection, String s) {
        return filterStudents(collection, Student::getGroup, s).collect(Collectors.toMap(Student::getLastName,
                Student::getFirstName, (n1, n2) -> Stream.of(n1, n2).min(Comparator.naturalOrder()).orElseThrow()));
    }
    
}
