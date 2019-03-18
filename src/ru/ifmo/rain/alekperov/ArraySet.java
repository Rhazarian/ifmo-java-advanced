package ru.ifmo.rain.alekperov;

import java.util.*;
import java.util.stream.Collectors;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {

    private final List<T> storage;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        this(Collections.emptyList());
    }

    public ArraySet(final Collection<? extends T> collection) {
        this(collection, null);
    }

    public ArraySet(final Collection<? extends T> collection, final Comparator<? super T> comparator) {
        this(new ArrayList<>(collection.stream().collect(Collectors.toCollection(() -> new TreeSet<T>(comparator)))),
                comparator);
    }

    private ArraySet(final List<T> storage, final Comparator<? super T> comparator) {
        this.storage = storage;
        this.comparator = comparator;
    }

    private boolean isIndexInRange(final int index) {
        return (index >= 0 && index < size());
    }

    private T get(final int index) {
        return storage.get(index);
    }

    private T getOrNull(final int index) {
        return isIndexInRange(index) ? get(index) : null;
    }

    private int binarySearch(final Object o) {
        final int result = Collections.binarySearch(storage, (T) o, comparator);
        return (result >= 0) ? result : (-1 - result);
    }

    private int ceilingIndex(final T e) {
        return binarySearch(e);
    }

    private int higherIndex(final T e) {
        final int result = binarySearch(e);
        return (result < size() && compare(e, get(result)) == 0) ? (result + 1) : result;
    }

    private int floorIndex(final T e) {
        final int result = binarySearch(e);
        return (result < size() && compare(e, get(result)) == 0) ? result : (result - 1);
    }

    private int lowerIndex(final T e) {
        return binarySearch(e) - 1;
    }

    private int compare(final T e1, final T e2) {
        if (e1 instanceof Comparable) {
            
        }
        return comparator == null ? ((Comparable<? super T>) e1).compareTo(e2) : comparator.compare(e1, e2);
    }

    private void checkBounds(final boolean fromStart, final T lo, final boolean toEnd, final T hi) {
        if (!fromStart && !toEnd && compare(lo, hi) > 0) {
            throw new IllegalArgumentException("fromElement > toElement");
        }
    }

    private int getSubSetStartIndex(final boolean fromStart, T lo, final boolean loInclusive) {
        return fromStart ? 0 : (loInclusive ? ceilingIndex(lo) : higherIndex(lo));
    }

    private int getSubSetEndIndex(final boolean toEnd, T hi, final boolean hiInclusive) {
        return toEnd ? size() : ((hiInclusive ? floorIndex(hi) : lowerIndex(hi)) + 1);
    }

    private NavigableSet<T> getSubSet(final boolean fromStart, final T fromElement, final boolean fromInclusive,
                                      final boolean toEnd, final T toElement, final boolean toInclusive) {
        checkBounds(fromStart, fromElement, toEnd, toElement);
        final int start = getSubSetStartIndex(fromStart, fromElement, fromInclusive);
        final int end = Math.max(start, getSubSetEndIndex(toEnd, toElement, toInclusive));
        return new ArraySet<>(storage.subList(start, end), comparator);
    }

    @Override
    public T lower(final T e) {
        return getOrNull(lowerIndex(e));
    }

    @Override
    public T floor(final T e) {
        return getOrNull(floorIndex(e));
    }

    @Override
    public T ceiling(final T e) {
        return getOrNull(ceilingIndex(e));
    }

    @Override
    public T higher(final T e) {
        return getOrNull(higherIndex(e));
    }

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public boolean contains(final Object o) {
        return Collections.binarySearch(storage, (T) o, comparator) >= 0;
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(storage).iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ReversedListView<>(storage), comparator == null ? null : comparator.reversed());
    }

    @Override
    public Iterator<T> descendingIterator() {
        return new ReversedListView<>(storage).iterator();
    }

    @Override
    public NavigableSet<T> subSet(final T fromElement, final boolean fromInclusive,
                                  final T toElement, final boolean toInclusive) {
        return getSubSet(false, fromElement, fromInclusive, false, toElement, toInclusive);
    }

    @Override
    public NavigableSet<T> subSet(final T fromElement, final T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public NavigableSet<T> headSet(final T toElement, final boolean inclusive) {
        return getSubSet(true, null, false, false, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> headSet(final T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public NavigableSet<T> tailSet(final T fromElement, final boolean inclusive) {
        return getSubSet(false, fromElement, inclusive, true, null, true);
    }

    @Override
    public NavigableSet<T> tailSet(final T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    private void requireNotEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public T first() {
        requireNotEmpty();
        return get(0);
    }

    @Override
    public T last() {
        requireNotEmpty();
        return get(size() - 1);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

}