package org.db.gora;

import java.util.ArrayList;
import java.util.List;

class DbTableInfo {
    public DbTableInfo(String tableName, String pkName) {
        this.tableName = tableName;
        this.pkName = pkName;
    }

    String tableName;
    String pkName;
    List<DbColumnInfo> columns = new ArrayList<DbColumnInfo>();
}
