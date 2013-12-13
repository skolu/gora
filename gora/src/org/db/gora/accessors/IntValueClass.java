package org.db.gora.accessors;

enum IntValueClass {
    Integer,
    Short,
    Byte;

    static IntValueClass resolveType(Class<?> type) {
        if (type == java.lang.Integer.TYPE) {
            return IntValueClass.Integer;
        } else if (type == java.lang.Short.TYPE) {
            return IntValueClass.Short;
        } else if (type == java.lang.Byte.TYPE) {
            return IntValueClass.Byte;
        } else {
            return IntValueClass.Integer;
        }
    }
}
