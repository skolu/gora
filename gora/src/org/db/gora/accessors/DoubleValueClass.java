package org.db.gora.accessors;

enum DoubleValueClass {
    Double,
    Float,
    BigDecimal;

    static DoubleValueClass resolveType(Class<?> type) {
        if (type == java.lang.Double.TYPE) {
            return DoubleValueClass.Double;
        } else if (type == java.lang.Float.TYPE) {
            return DoubleValueClass.Float;
        } else if (type == java.math.BigDecimal.class) {
            return DoubleValueClass.BigDecimal;
        } else {
            return DoubleValueClass.Double;
        }
    }
}
