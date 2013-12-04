package org.db.gora;

import java.util.List;

class DbIndexInfo {
    String indexName;
    String tableName;
    boolean isUnique;
    List<String> columns;
}
