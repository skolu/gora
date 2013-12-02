package org.db.gora;

import java.util.Iterator;

/**
 * Created by skolupaev on 12/1/13.
 */
public interface ClosableIterator<T> extends Iterator<T> {
    void close();
}
