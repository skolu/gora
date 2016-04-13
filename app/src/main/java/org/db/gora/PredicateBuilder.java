/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.db.gora;

import android.database.DatabaseUtils;

import java.util.ArrayList;
import java.util.Date;

/**
  * Creates SQL Where and OrderBy clauses
  * See {@link org.db.gora.SqliteManager}
  *
  * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

public class PredicateBuilder {
    final TableData mTable;

    PredicateBuilder(TableData table) {
        mTable = table;
    }

    /**
     * Class that holds OrderBy clause structure
     */
    public final class OrderByClause {
        final StringBuilder mOrderBy;

        OrderByClause() {
            mOrderBy = new StringBuilder();
        }

        public void clear() {
            mOrderBy.setLength(0);
        }

        public String getOrderByClause() {
            return mOrderBy.toString();
        }

        public OrderByClause orderBy(String name, boolean asc) throws DataIntegrityException {
            FieldData fd = mTable.getFieldByName(name);
            if (fd == null) {
                throw new DataIntegrityException(String.format("Table %s does not have column %s", mTable.tableName, name));
            }
            clear();

            mOrderBy.append(String.format("%s %s", fd.columnName, asc ? "ASC" : "DESC"));

            return this;
        }

        public OrderByClause thenBy(String name, boolean asc) throws DataIntegrityException {
            FieldData fd = mTable.getFieldByName(name);
            if (fd == null) {
                throw new DataIntegrityException(String.format("Table %s does not have column %s", mTable.tableName, name));
            }
            if (mOrderBy.length() > 0) {
                mOrderBy.append(", ");
            }
            mOrderBy.append(String.format("%s %s", fd.columnName, asc ? "ASC" : "DESC"));
            return this;
        }

        public OrderByClause orderBy(String name) throws DataIntegrityException {
            return orderBy(name, true);
        }
        public OrderByClause thenBy(String name) throws DataIntegrityException {
            return thenBy(name, true);
        }
    }
    /**
     * Returns an instance of {@link OrderByClause}
     *
     * @return instance if {@link OrderByClause}
     */
    public OrderByClause orderBy() {
        return new OrderByClause();
    }

    /**
     * Returns an instance of {@link WhereClause}
     *
     * @return instance if {@link WhereClause}
     */
    public WhereClause where() {
        return new WhereClause();
    }

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
            FieldData fd = mTable.getFieldByName(field);
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
            FieldData fd = mTable.getFieldByName(field);
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
            FieldData fd = mTable.getFieldByName(field);
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
            FieldData fd = mTable.getFieldByName(field);
            if (fd != null) field = fd.columnName;
            return addCriteria(new WhereCriteria(this, field, CriteriaOperation.GREATER, new Object[]{value}));
        }

        /**
         * Creates range (SQL: BETWEEN AND) criteria
         *
         * @param field  criteria name. Can be either database column name or class field name.
         * @param from criteria from value.
         * @param to criteria to value.
         * @return parsed criteria
         */
        public WhereCriteria range(String field, Object from, Object to) {
            FieldData fd = mTable.getFieldByName(field);
            if (fd != null) field = fd.columnName;
            return addCriteria(new WhereCriteria(this, field, CriteriaOperation.RANGE, new Object[]{from, to}));
        }

        /**
         * Creates set (SQL: IN) criteria
         *
         * @param field criteria name. Can be either database column name or class field name.
         * @param values array of criteria values.
         * @return parsed criteria
         */
        public WhereCriteria set(String field, Object[] values) {
            FieldData fd = mTable.getFieldByName(field);
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

        /**
         * Builds Where clause string
         *
         * @return WHERE clause
         */
        public String getWhereClause() {
            StringBuilder builder = new StringBuilder();
            builder.setLength(0);
            for (int i = 0; i < mOrList.size(); ++i) {
                int currentPosition = builder.length();

                WhereCriteria criteria = mOrList.get(i);
                while (criteria != null) {
                    int criteriaPosition = builder.length();

                    switch (criteria.getOperation()) {
                        case EQUAL:
                            builder.append(criteria.mFieldName);
                            if (criteria.mValues[0] == null) {
                                builder.append(criteria.mExclude ? " IS NOT NULL " : " IS NULL ");
                            } else {
                                builder.append(criteria.mExclude ? " <> " : " = ");
                            }
                            builder.append(toSqlString(criteria.mValues[0]));
                            break;

                        case LIKE:
                            builder.append(criteria.mFieldName);
                            builder.append(criteria.mExclude ? " NOT LIKE " : " LIKE ");
                            builder.append(toSqlString(criteria.mValues[0]));
                            break;

                        case LESS:
                            builder.append(criteria.mFieldName);
                            builder.append(criteria.mExclude ? " >= " : " < ");
                            builder.append(toSqlString(criteria.mValues[0]));
                            break;

                        case GREATER:
                            builder.append(criteria.mFieldName);
                            builder.append(criteria.mExclude ? " <= " : " > ");
                            builder.append(toSqlString(criteria.mValues[0]));
                            break;

                        case RANGE:
                            builder.append(criteria.mFieldName);
                            if (criteria.mExclude) {
                                builder.append(" NOT");
                            }
                            builder.append(" BETWEEN ");
                            builder.append(toSqlString(criteria.mValues[0]));
                            builder.append(" AND ");
                            builder.append(toSqlString(criteria.mValues[1]));

                            break;

                        case SET:
                            builder.append(criteria.mFieldName);
                            if (criteria.mExclude) {
                                builder.append(" NOT");
                            }
                            builder.append(" IN (");
                            for (int j = 0; j < criteria.mValues.length; ++j) {
                                if (j > 0) {
                                    builder.append(", ");
                                }
                                builder.append(toSqlString(criteria.mValues[j]));
                            }
                            builder.append(")");
                            break;

                        default:
                            break;
                    }

                    if (builder.length() > criteriaPosition) {
                        builder.insert(criteriaPosition, '(');
                        builder.append(")");

                        if (criteriaPosition > currentPosition) {
                            builder.insert(criteriaPosition, " AND ");
                        }
                    }

                    criteria = criteria.nextCriteria;
                }

                if (currentPosition < builder.length()) {
                    builder.insert(currentPosition, '(');
                    builder.append(')');
                    if (currentPosition > 0) {
                        builder.insert(currentPosition, " OR ");
                    }
                }
            }
            return builder.toString();
        }


        private WhereCriteria mLastCriteria;
        private ArrayList<WhereCriteria> mOrList = new ArrayList<>();
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

    static String toSqlString(Object value) {
        if (value == null) return "NULL";
        Class<?> type = value.getClass();

        if (type == Long.TYPE) return Long.toString((Long) value);
        if (type == Integer.TYPE) return Integer.toString((Integer) value);
        if (type == Short.TYPE) return Short.toString((Short) value);
        if (type == Byte.TYPE) return Byte.toString((Byte) value);
        if (type == Boolean.TYPE) return (Boolean) value ? "1" : "0";
        if (type == Double.TYPE) return Double.toString((Double) value);
        if (type == Float.TYPE) return Float.toString((Float) value);
        if (type == String.class) return DatabaseUtils.sqlEscapeString((String) value);
        if (type == Date.class) return Long.toString(((Date) value).getTime());
        if (type.isEnum()) return DatabaseUtils.sqlEscapeString(((Enum) value).name());

        return value.toString();
    }
}

