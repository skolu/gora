package org.db.gora;

/**
 * User: skolupaev
 * Date: 12/3/13
 */
public interface SqliteEvent {
    void onRead();
    boolean onWrite();
}
