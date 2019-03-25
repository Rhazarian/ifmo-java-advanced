package ru.ifmo.rain.alekperov.arrayset;

import java.util.AbstractList;
import java.util.List;

public class ReversedListView<E> extends AbstractList<E> {

    private final List<E> list;
    private final boolean reversed;

    ReversedListView(final List<E> list) {
        this.list = list instanceof ReversedListView ? ((ReversedListView<E>) list).list : list;
        this.reversed = !(list instanceof ReversedListView) || !((ReversedListView<E>) list).reversed;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public E get(final int index) {
        return reversed ? list.get(size() - 1 - index) : list.get(index);
    }

}
