package io.github.chatzikalymnios.lfds;

public interface LockFreeStack<E> {
    void push(E data);

    E pop();

    void reportStats();
}
