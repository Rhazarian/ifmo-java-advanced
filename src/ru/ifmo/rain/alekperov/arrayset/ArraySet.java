package ru.ifmo.rain.alekperov.arrayset;

import java.util.*;
import java.util.stream.Collectors;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {

    private final List<E> storage;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        this(Collections.emptyList());
    }

    public ArraySet(final Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet(final Collection<? extends E> collection, final Comparator<? super E> comparator) {
        this(new ArrayList<>(collection.stream().collect(Collectors.toCollection(() -> new TreeSet<E>(comparator)))),
                comparator);
    }

    private ArraySet(final List<E> storage, final Comparator<? super E> comparator) {
        this.storage = storage;
        this.comparator = comparator;
    }

    private boolean isIndexInRange(final int index) {
        return (index >= 0 && index < size());
    }

    private E get(final int index) {
        return storage.get(index);
    }

    private E getOrNull(final int index) {
        return isIndexInRange(index) ? get(index) : null;
    }

    private int binarySearch(final E e) {
        final int result = Collections.binarySearch(storage, e, comparator);
        return (result >= 0) ? result : (-1 - result);
    }

    private int ceilingIndex(final E e) {
        return binarySearch(e);
    }

    private int higherIndex(final E e) {
        final int result = binarySearch(e);
        return (result < size() && compare(e, get(result)) == 0) ? (result + 1) : result;
    }

    private int floorIndex(final E e) {
        final int result = binarySearch(e);
        return (result < size() && compare(e, get(result)) == 0) ? result : (result - 1);
    }

    private int lowerIndex(final E e) {
        return binarySearch(e) - 1;
    }

    private int compare(final E e1, final E e2) {
        return Collections.reverseOrder(comparator).reversed().compare(e1, e2);
    }

    private void checkBounds(final boolean fromStart, final E lo, final boolean toEnd, final E hi) {
        if (!fromStart && !toEnd && compare(lo, hi) > 0) {
            throw new IllegalArgumentException("fromElement > toElement");
        }
    }

    private int getSubSetStartIndex(final boolean fromStart, final E lo, final boolean loInclusive) {
        return fromStart ? 0 : (loInclusive ? ceilingIndex(lo) : higherIndex(lo));
    }

    private int getSubSetEndIndex(final boolean toEnd, final E hi, final boolean hiInclusive) {
        return toEnd ? size() : ((hiInclusive ? floorIndex(hi) : lowerIndex(hi)) + 1);
    }

    private NavigableSet<E> getSubSet(final boolean fromStart, final E fromElement, final boolean fromInclusive,
                                      final boolean toEnd, final E toElement, final boolean toInclusive) {
        checkBounds(fromStart, fromElement, toEnd, toElement);
        final int start = getSubSetStartIndex(fromStart, fromElement, fromInclusive);
        final int end = getSubSetEndIndex(toEnd, toElement, toInclusive);
        return new ArraySet<>(storage.subList(start, Math.max(start, end)), comparator);
    }

    @Override
    public E lower(final E e) {
        return getOrNull(lowerIndex(e));
    }

    @Override
    public E floor(final E e) {
        return getOrNull(floorIndex(e));
    }

    @Override
    public E ceiling(final E e) {
        return getOrNull(ceilingIndex(e));
    }

    @Override
    public E higher(final E e) {
        return getOrNull(higherIndex(e));
    }

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public boolean contains(final Object o) {
        @SuppressWarnings("unchecked")
            final int pos = Collections.binarySearch(storage, (E) o, comparator);
        return pos >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(storage).iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new ReversedListView<>(storage), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new ReversedListView<>(storage).iterator();
    }

    @Override
    public NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive,
                                  final E toElement, final boolean toInclusive) {
        return getSubSet(false, fromElement, fromInclusive, false, toElement, toInclusive);
    }

    @Override
    public NavigableSet<E> subSet(final E fromElement, final E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public NavigableSet<E> headSet(final E toElement, final boolean inclusive) {
        return getSubSet(true, null, false, false, toElement, inclusive);
    }

    @Override
    public NavigableSet<E> headSet(final E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public NavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
        return getSubSet(false, fromElement, inclusive, true, null, true);
    }

    @Override
    public NavigableSet<E> tailSet(final E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    private void requireNotEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public E first() {
        requireNotEmpty();
        return get(0);
    }

    @Override
    public E last() {
        requireNotEmpty();
        return get(size() - 1);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

}