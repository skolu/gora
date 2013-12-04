package org.db.gora;

import java.util.ArrayList;
import java.util.Date;

public class PredicateBuilder {
    final TableData mTable;
    final WhereClause mWhereClause;

    PredicateBuilder(TableData table) {
        mTable = table;
        mWhereClause = new WhereClause();
    }

    /**
     * Builds Where clause string
     *
     * @return WHERE clause
     */
    public String getWhereClause() {
        if (mWhereClause == null) return null;
        String where;
        synchronized (sBuilder) {
            sBuilder.setLength(0);
            for (int i = 0; i < mWhereClause.mOrList.size(); ++i) {
                int currentPosition = sBuilder.length();

                WhereCriteria criteria = mWhereClause.mOrList.get(i);
                while (criteria != null) {
                    int criteriaPosition = sBuilder.length();

                    switch (criteria.getOperation()) {
                        case EQUAL:
                            sBuilder.append(criteria.mFieldName);
                            if (criteria.mValues[0] == null) {
                                sBuilder.append(criteria.mExclude ? " IS NOT NULL " : " IS NULL ");
                            } else {
                                sBuilder.append(criteria.mExclude ? " <> " : " = ");
                            }
                            sBuilder.append(toSqlString(criteria.mValues[0]));
                            break;

                        case LIKE:
                            sBuilder.append(criteria.mFieldName);
                            sBuilder.append(criteria.mExclude ? " NOT LIKE " : " LIKE ");
                            sBuilder.append(toSqlString(criteria.mValues[0]));
                            break;

                        case LESS:
                            sBuilder.append(criteria.mFieldName);
                            sBuilder.append(criteria.mExclude ? " >= " : " < ");
                            sBuilder.append(toSqlString(criteria.mValues[0]));
                            break;

                        case GREATER:
                            sBuilder.append(criteria.mFieldName);
                            sBuilder.append(criteria.mExclude ? " <= " : " > ");
                            sBuilder.append(toSqlString(criteria.mValues[0]));
                            break;

                        case RANGE:
                            sBuilder.append(criteria.mFieldName);
                            if (criteria.mExclude) {
                                sBuilder.append(" NOT");
                            }
                            sBuilder.append(" BETWEEN ");
                            sBuilder.append(toSqlString(criteria.mValues[0]));
                            sBuilder.append(" AND ");
                            sBuilder.append(toSqlString(criteria.mValues[1]));

                            break;

                        case SET:
                            sBuilder.append(criteria.mFieldName);
                            if (criteria.mExclude) {
                                sBuilder.append(" NOT");
                            }
                            sBuilder.append(" IN (");
                            for (int j = 0; j < criteria.mValues.length; ++j) {
                                if (j > 0) {
                                    sBuilder.append(", ");
                                }
                                sBuilder.append(toSqlString(criteria.mValues[j]));
                            }
                            sBuilder.append(")");
                            break;
                            
                         default:
                        	 break;
                    }

                    if (sBuilder.length() > criteriaPosition) {
                        sBuilder.insert(criteriaPosition, '(');
                        sBuilder.append(")");

                        if (criteriaPosition > currentPosition) {
                            sBuilder.insert(criteriaPosition, " AND ");
                        }
                    }

                    criteria = criteria.nextCriteria;
                }

                if (currentPosition < sBuilder.length()) {
                    sBuilder.insert(currentPosition, '(');
                    sBuilder.append(')');
                    if (currentPosition > 0) {
                        sBuilder.insert(currentPosition, " OR ");
                    }
                }
            }
            where = sBuilder.toString();
        }
        return where;
    }

    /**
     * Returns the instance of {@link WhereClause}
     *
     * @return instance if {@link WhereClause}
     */
    public WhereClause where() {
        return mWhereClause;
    }

    FieldData resolveFieldByName(String fieldName) {
        if (fieldName == null) return null;
        if (fieldName.length() == 0) return null;
        if (mTable != null) {
            for (int i = 0; i < mTable.fields.length; ++i) {
                FieldData fd = mTable.fields[i];
                if (fd.columnName.equalsIgnoreCase(fieldName)) {
                    return fd;
                }
            }
        }
        return null;
    }

    /**
     * Class that holds Where clause structure
     *
     * @author Sergey_Kolupaev@Intuit.com
     */
    public final class WhereClause {

        /**
         * Clears Where clause structure
         */
        public void clear() {
            mOrList.clear();
            mLastCriteria = null;
        }

        /**
         * Creates equal (SQL: =) criteria
         *
         * @param field criteria name. Can be either database column name or class field name.
         * @param value criteria value.
         * @return parsed criteria
         */
        public WhereCriteria eq(String field, Object value) {
            FieldData fd = PredicateBuilder.this.resolveFieldByName(field);
            if (fd != null) field = fd.columnName;
            return addCriteria(new WhereCriteria(this, field, CriteriaOperation.EQUAL, new Object[]{value}));
        }

        /**
         * Creates like (SQL: LIKE) criteria
         *
         * @param field criteria name. Can be either database column name or class field name.
         * @param value criteria value.
         * @return parsed criteria
         */
        public WhereCriteria like(String field, String value) {
            FieldData fd = PredicateBuilder.this.resolveFieldByName(field);
            if (fd != null) field = fd.columnName;
            return addCriteria(new WhereCriteria(this, field, CriteriaOperation.LIKE, new Object[]{value}));
        }

        /**
         * Creates less than (SQL: <) criteria
         *
         * @param field criteria name. Can be either database column name or class field name.
         * @param value criteria value.
         * @return parsed criteria
         */
        public WhereCriteria lt(String field, Object value) {
            FieldData fd = PredicateBuilder.this.resolveFieldByName(field);
            if (fd != null) field = fd.columnName;
            return addCriteria(new WhereCriteria(this, field, CriteriaOperation.LESS, new Object[]{value}));
        }

        /**
         * Creates greater than (SQL: >) criteria
         *
         * @param field criteria name. Can be either database column name or class field name.
         * @param value criteria value.
         * @return parsed criteria
         */
        public WhereCriteria gt(String field, Object value) {
            FieldData fd = PredicateBuilder.this.resolveFieldByName(field);
            if (fd != null) field = fd.columnName;
            return addCriteria(new WhereCriteria(this, field, CriteriaOperation.GREATER, new Object[]{value}));
        }

        /**
         * Creates range (SQL: BETWEEN AND) criteria
         *
         * @param field  criteria name. Can be either database column name or class field name.
         * @param value1 criteria from value.
         * @param value1 criteria to value.
         * @return parsed criteria
         */
        public WhereCriteria range(String field, Object value1, Object value2) {
            FieldData fd = PredicateBuilder.this.resolveFieldByName(field);
            if (fd != null) field = fd.columnName;
            return addCriteria(new WhereCriteria(this, field, CriteriaOperation.RANGE, new Object[]{value1, value2}));
        }

        /**
         * Creates set (SQL: IN) criteria
         *
         * @param field criteria name. Can be either database column name or class field name.
         * @param values array of criteria values.
         * @return parsed criteria
         */
        public WhereCriteria set(String field, Object[] values) {
            FieldData fd = PredicateBuilder.this.resolveFieldByName(field);
            if (fd != null) field = fd.columnName;
            return addCriteria(new WhereCriteria(this, field, CriteriaOperation.SET, values));
        }

        private WhereCriteria addCriteria(WhereCriteria criteria) {
            if (mLastCriteria != null) {
                mLastCriteria.nextCriteria = criteria;
            } else {
                mOrList.add(criteria);
            }
            mLastCriteria = criteria;
            return mLastCriteria;
        }

        private WhereCriteria mLastCriteria;
        private ArrayList<WhereCriteria> mOrList = new ArrayList<WhereCriteria>();
    }

    /**
     * Class that holds a single criteria.
     *
     * @author Sergey_Kolupaev@Intuit.com
     */
    public final class WhereCriteria {
        private WhereCriteria(WhereClause owner, String field, CriteriaOperation operation, Object[] values) {
            mOwner = owner;
            mExclude = false;
            mOperation = operation;
            mFieldName = field;
            mValues = values != null ? values : new Object[0];

            if (mFieldName == null) {
                mOperation = CriteriaOperation.NOP;
            }

            if (mValues.length == 0) {
                mOperation = CriteriaOperation.NOP;
            }
            switch (mOperation) {
                case EQUAL:
                    if (mValues.length > 1) {
                        mOperation = CriteriaOperation.SET;
                    }
                    break;

                case SET:
                    if (mValues.length == 1) {
                        mOperation = CriteriaOperation.EQUAL;
                    }
                    break;

                case RANGE:
                    if (mValues.length == 1) {
                        mOperation = mValues[0] == null ? CriteriaOperation.NOP : CriteriaOperation.GREATER;
                    } else if (mValues[0] == null && mValues[1] == null) {
                        mOperation = CriteriaOperation.NOP;
                    } else if (mValues[0] == null || mValues[1] == null) {
                        if (mValues[0] == null) {
                            mValues[0] = mValues[1];
                            mOperation = CriteriaOperation.GREATER;
                            mExclude = true;
                        } else {
                            mOperation = CriteriaOperation.LESS;
                            mExclude = true;
                        }
                    }

                    break;

                case LESS:
                case GREATER:
                    if (mValues[0] == null) {
                        mOperation = CriteriaOperation.NOP;
                    }
                    break;

                case LIKE:
                    if (mValues[0] == null) {
                        mOperation = CriteriaOperation.EQUAL;
                    } else {
                        if (mValues[0] instanceof String) {
                            // OK
                        } else {
                            mOperation = CriteriaOperation.NOP;
                        }
                    }
                    break;
            }
        }

        /**
         * Negates the criteria.
         *
         * @return this instance
         */
        public WhereCriteria exclude() {
            mExclude = true;
            return this;
        }

        /**
         * Adds AND
         *
         * @return {@link WhereClause} instance
         */
        public WhereClause and() {
            return mOwner;
        }

        /**
         * Adds OR
         *
         * @return {@link WhereClause} instance
         */
        public WhereClause or() {
            mOwner.mLastCriteria = null;
            return mOwner;
        }

        protected CriteriaOperation getOperation() {
            return mOperation;
        }

        private String mFieldName;
        private boolean mExclude;
        private CriteriaOperation mOperation;
        private Object[] mValues;
        public WhereCriteria nextCriteria;
        private WhereClause mOwner;
    }

    protected enum CriteriaOperation {NOP, EQUAL, LIKE, LESS, GREATER, RANGE, SET}


    private final static StringBuilder sBuilder = new StringBuilder();

    public static String toSqlString(Object value) {
        if (value == null) return "NULL";

        if (value instanceof Long) return Long.toString((Long) value);

        if (value instanceof String) {
            String str = (String) value;
            synchronized (sBuilder) {
                int saved = sBuilder.length();
                try {
                    sBuilder.append(SqlEscape);
                    for (int i = 0; i < str.length(); ++i) {
                        char ch = str.charAt(i);
                        sBuilder.append(ch);
                        if (ch == SqlEscape) {
                            sBuilder.append(SqlEscape);
                        }

                    }
                    sBuilder.append(SqlEscape);
                    str = sBuilder.substring(saved);
                }
                finally {
                    sBuilder.setLength(saved);
                }
            }
            return str;
        }

        if (value instanceof Integer) return Integer.toString((Integer) value);
        if (value instanceof Short) return Short.toString((Short) value);
        if (value instanceof Byte) return Byte.toString((Byte) value);
        if (value instanceof Boolean) return (Boolean) value ? "1" : "0";
        if (value instanceof Date) return Long.toString(((Date) value).getTime());
        if (value instanceof Double) return Double.toString((Double) value);
        if (value instanceof Float) return Float.toString((Float) value);

        return value.toString();
    }

    private static final char SqlEscape = '\'';
}

