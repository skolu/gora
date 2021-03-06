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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
  * Provides frequently used queries
  *
  * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

final class TableQueryBuilder {
	
	final TableData tableData; 
	public TableQueryBuilder(TableData tableData) {
		this.tableData = tableData;
	}

    private String selectByIdQuery = null;
    public String getSelectByIdQuery() {
        if (selectByIdQuery == null) {
            selectByIdQuery = getSelectClause();
            selectByIdQuery = selectByIdQuery.concat(String.format(Locale.getDefault(), " FROM %s AS t%d", tableData.tableName, tableData.tableNo));
            selectByIdQuery = selectByIdQuery.concat(String.format(Locale.getDefault(), " WHERE t%d.%s = ?", tableData.tableNo, tableData.primaryKey.columnName));
        }
        return selectByIdQuery;
    }

    private String deleteByIdWhereClause = null;
    public String getDeleteByIdWhereClause() {
        if (deleteByIdWhereClause == null) {
            deleteByIdWhereClause = String.format("%s = ?", tableData.primaryKey.columnName);
        }
        return deleteByIdWhereClause;
    }

    private String selectClause;
    String getSelectClause() {
        if (selectClause == null) {
            StringBuilder builder = new StringBuilder();
            builder.append("SELECT ");
            for (int i = 0; i < tableData.fields.length; ++i) {
                FieldData field = tableData.fields[i];
                if (i > 0) builder.append(", ");
                builder.append(String.format(Locale.getDefault(), "t%d.%s", tableData.tableNo, field.columnName));
            }
            selectClause = builder.toString();
        }
        return selectClause;
    }

    private String selectQuery;
    String getSelectQuery() {
        if (selectQuery == null) {
            selectQuery = String.format(Locale.getDefault(), "%s FROM %s AS t%d ", getSelectClause(), tableData.tableName, tableData.tableNo);
        }
        return selectQuery;
    }

	private String insertClause;
	String getInsertClause() {
		if (insertClause == null) {
			StringBuilder builder = new StringBuilder();
			builder.append("INSERT INTO ");
			builder.append(tableData.tableName);
			builder.append("( ");
			boolean addComma = false;
			for (FieldData field: tableData.fields) {
				if (field != tableData.primaryKey) {
					if (addComma) {
						builder.append(", ");
					} else {
						addComma = true;
					}
					builder.append(field.columnName);
				}
			}
			builder.append(") VALUES ( ");
			int paramNo = 1;
			for (FieldData field: tableData.fields) {
				if (field != tableData.primaryKey) {
					if (paramNo > 1) {
						builder.append(", ");
					}
					builder.append(":");
					builder.append(paramNo);
					paramNo += 1;
				}
			}
			builder.append(")");

			insertClause = builder.toString();
		}
		return insertClause;
	}

	private String updateClause;
	String getUpdateClause() {
		if (updateClause == null) {
			StringBuilder builder = new StringBuilder();
			builder.append("UPDATE ");
			builder.append(tableData.tableName);
			builder.append(" SET ");
			int paramNo = 1;
			for (FieldData field: tableData.fields) {
				if (field != tableData.primaryKey) {
					if (paramNo > 1) {
						builder.append(", ");
					}
					builder.append(field.columnName);
					builder.append(" = :");
					builder.append(paramNo);

					paramNo += 1;
				}
			}
			builder.append(" WHERE ");
			builder.append(tableData.primaryKey.columnName);
			builder.append(" = :");
			builder.append(paramNo);

			updateClause = builder.toString();
		}
		return updateClause;
	}


    final Map<Class<?>, LinkedQueryBuilder> linkedBuilders = new HashMap<>();

	final class LinkedQueryBuilder {
		final TableData[] pathToId;
		public LinkedQueryBuilder(TableData[] pathToId) {
			this.pathToId = pathToId != null ? pathToId : new TableData[0];
		}
		
		public TableData getTableData() {
			return tableData;
		}

        public TableData getParentTableData() {
      	    return pathToId.length > 0 ? pathToId[0] : null;
      	}

		private String fromWhereByLinkedIdClause = null;
		public String getFromWhereByLinkedIdClause() {
			if (fromWhereByLinkedIdClause == null) {
	    		StringBuilder builder = new StringBuilder();
	    		builder.append(String.format(Locale.getDefault(), " FROM %s AS t%d", tableData.tableName, tableData.tableNo));
	    		
	    		if (pathToId.length > 0) {
	    			TableData lastTable = tableData;
	    			for (int i = 0; i < (pathToId.length - 1); i++) {
	    				TableData thisTable = pathToId[i];
	        			builder.append(String.format(Locale.getDefault(), " INNER JOIN %s AS t%d ON t%d.%s = t%d.%s",
	        					thisTable.tableName, thisTable.tableNo,
	        					thisTable.tableNo, thisTable.primaryKey.columnName,
	        					lastTable.tableNo, lastTable.foreignKey.columnName
	        					));
	        			lastTable = thisTable;
	    			}
	        		builder.append(String.format(Locale.getDefault(), " WHERE t%d.%s = ?", lastTable.tableNo, lastTable.foreignKey.columnName));
	    		} else {
	        		builder.append(String.format(Locale.getDefault(), " WHERE t%d.%s = ?", tableData.tableNo, tableData.primaryKey.columnName));
	    		}

	    		fromWhereByLinkedIdClause = builder.toString();
			}
			return fromWhereByLinkedIdClause;
		}

        private String selectByLinkedIdQuery = null;
		public String getSelectByIdQuery() {
			if (selectByLinkedIdQuery == null) {
				selectByLinkedIdQuery = String.format(Locale.getDefault(), "%s %s", getSelectClause(), getFromWhereByLinkedIdClause());
			}
			return selectByLinkedIdQuery;
		}
		
	    private String selectIdByLinkedIdQuery = null;
	    String getSelectIdByLinkedIdQuery() {
	    	if (selectIdByLinkedIdQuery == null) {
	    		selectIdByLinkedIdQuery = String.format(Locale.getDefault(),
						"SELECT t%d.%s %s", tableData.tableNo, tableData.primaryKey.columnName, getFromWhereByLinkedIdClause());
	    	}
	    	return selectIdByLinkedIdQuery;
	    }
	    
		
		private String deleteByLinkedIdWhereClause = null;
		public String getDeleteByIdWhereClause() {
			if (deleteByLinkedIdWhereClause == null) {
				StringBuilder builder = new StringBuilder();
				if (pathToId.length > 0) {
					if (pathToId.length > 1) {
						builder.append(String.format("%s IN (", tableData.foreignKey.columnName));
						int openBrackets = 1;
						for (int i = 0; i < (pathToId.length - 2); i++) {
							TableData thisTable = pathToId[i];
							++openBrackets;
							builder.append(String.format("SELECT %s FROM %s WHERE %s IN (", 
									thisTable.primaryKey.columnName, thisTable.tableName, thisTable.foreignKey.columnName));
						}            			
						TableData lastTable = pathToId[pathToId.length - 2];
						builder.append(String.format("SELECT %s FROM %s WHERE %s = ?", 
								lastTable.primaryKey.columnName, lastTable.tableName, lastTable.foreignKey.columnName));

						while (openBrackets > 0) {
							builder.append(")");
							--openBrackets;
						}
					} else {
						builder.append(String.format("%s = ?", tableData.foreignKey.columnName));
					} 
				} else {
					builder.append(String.format("%s = ?", tableData.primaryKey.columnName));
				}
				deleteByLinkedIdWhereClause = builder.toString();
			}
			return deleteByLinkedIdWhereClause;
		}
	}
	
}
