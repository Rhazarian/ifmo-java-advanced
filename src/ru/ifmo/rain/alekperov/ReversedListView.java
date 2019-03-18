package ru.ifmo.rain.alekperov;

import java.util.AbstractList;
import java.util.List;

public class ReversedListView<T> extends AbstractList<T> {

    private final List<T> list;
    private final boolean reversed;

    ReversedListView(final List<T> list) {
        this.list = list instanceof ReversedListView ? ((ReversedListView<T>) list).list : list;
        this.reversed = !(list instanceof ReversedListView) || !((ReversedListView<T>) list).reversed;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public T get(final int index) {
        return reversed ? list.get(size() - 1 - index) : list.get(index);
    }

}
