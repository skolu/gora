package org.db.gora.sqlite;

import java.util.HashMap;
import java.util.Map;

final class TableQueryBuilder {
	
	final TableData tableData; 
	public TableQueryBuilder(TableData tableData) {
		this.tableData = tableData;
	}
	
	final class LinkedQueryBuilder {
		final TableData[] pathToId;
		public LinkedQueryBuilder(TableData[] pathToId) {
			this.pathToId = pathToId != null ? pathToId : new TableData[0];
		}
		
		public TableData getTableData() {
			return tableData;
		}

		private String fromWhereByLinkedIdClause = null;
		public String getFromWhereByLinkedIdClause() {
			if (fromWhereByLinkedIdClause != null) {
	    		StringBuilder builder = new StringBuilder();
	    		builder.append(String.format(" FROM %s AS t%d", tableData.tableName, tableData.tableNo));
	    		
	    		if (pathToId.length > 0) {
	    			TableData lastTable = tableData;
	    			for (int i = 0; i < (pathToId.length - 1); i++) {
	    				TableData thisTable = pathToId[i];
	        			builder.append(String.format(" INNER JOIN %s AS t%d ON t%d.%s = t%d.%s", 
	        					thisTable.tableName, thisTable.tableNo,
	        					thisTable.tableNo, thisTable.primaryKey.columnName,
	        					lastTable.tableNo, lastTable.foreignKey.columnName
	        					));
	        			lastTable = thisTable;
	    			}
	        		builder.append(String.format(" WHERE t%d.%s = ?", lastTable.tableNo, lastTable.foreignKey.columnName));
	    		} else {
	        		builder.append(String.format(" WHERE t%d.%s = ?", tableData.tableNo, tableData.primaryKey.columnName));
	    		}

	    		fromWhereByLinkedIdClause = builder.toString();
			}
			return fromWhereByLinkedIdClause;
		}
		
		
		private String selectByLinkedIdQuery = null;
		public String getSelectByIdQuery() {
			if (selectByLinkedIdQuery != null) {
				selectByLinkedIdQuery = String.format("%s %s", getSelectClause(), getFromWhereByLinkedIdClause());
			}
			return selectByLinkedIdQuery;
		}
		
	    private String selectIdByLinkedIdQuery = null;
	    String getSelectIdByLinkedIdQuery() {
	    	if (selectIdByLinkedIdQuery != null) {
	    		selectIdByLinkedIdQuery = String.format(
	    				"SELECT t%d.%s %s", tableData.tableNo, tableData.primaryKey.columnName, getFromWhereByLinkedIdClause());
	    	}
	    	return selectIdByLinkedIdQuery;
	    }
	    
		
		private String deleteByLinkedIdWhereClause = null;
		public String getDeleteByIdWhereClause() {
			if (deleteByLinkedIdWhereClause != null) {
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
	
	private String selectByIdQuery = null;
	public String getSelectByIdQuery() {
		if (selectByIdQuery != null) {
    		StringBuilder builder = new StringBuilder();
    		builder.append(getSelectClause());
    		builder.append(String.format(" FROM %s AS t%d", tableData.tableName, tableData.tableNo));
    		builder.append(String.format(" WHERE t%d.%s = ?", tableData.tableNo, tableData.primaryKey.columnName));
    		selectByIdQuery = builder.toString();
		}
		return selectByIdQuery;
	}
	
	private String deleteByIdWhereClause = null;
	public String getDeleteByIdWhereClause() {
		if (deleteByIdWhereClause != null) {
			StringBuilder builder = new StringBuilder();
			builder.append(String.format("%s = ?", tableData.primaryKey.columnName));
			deleteByIdWhereClause = builder.toString();
		}
		return deleteByIdWhereClause;
	}
	
	private String selectClause;
    String getSelectClause() {
    	if (selectClause != null) {
    		StringBuilder builder = new StringBuilder();
    		builder.append("SELECT ");
    		for (int i = 0; i < tableData.fields.length; ++i) {
    			FieldData field = tableData.fields[i];
    			if (i > 0) builder.append(", ");
    			builder.append(String.format("t%d.%s", tableData.tableNo, field.columnName));
    		}
    		selectClause = builder.toString();
    	}
    	return selectClause;
    }

    private String updateWhereClause;
    String getUpdateWhereClause() {
    	if (updateWhereClause != null) {
    		updateWhereClause = String.format("%s = ?", tableData.primaryKey.columnName);
    	}
    	return updateWhereClause;
    }
    
    
    final Map<Class<?>, LinkedQueryBuilder> linkedBuilders = new HashMap<Class<?>, LinkedQueryBuilder>();
}
