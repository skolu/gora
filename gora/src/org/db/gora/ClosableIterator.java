package org.db.gora;

import java.util.Iterator;

public interface ClosableIterator<T> extends Iterator<T> {
    void close();
}
