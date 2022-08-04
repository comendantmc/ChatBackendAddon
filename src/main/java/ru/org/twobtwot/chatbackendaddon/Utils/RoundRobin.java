package ru.org.twobtwot.chatbackendaddon.Utils;

import java.util.Iterator;
import java.util.List;

//https://stackoverflow.com/a/2041772
public class RoundRobin<T> implements Iterable<T> {
    private List<T> coll;

    public RoundRobin(List<T> coll) { this.coll = coll; }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                T res = coll.get(index);
                index = (index + 1) % coll.size();
                return res;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }
}