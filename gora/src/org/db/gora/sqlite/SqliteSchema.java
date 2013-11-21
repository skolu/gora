package org.db.gora.sqlite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SqliteSchema {
	
	final Map<Class<?>, TableData> tableMap = new HashMap<Class<?>, TableData>();
	public TableData getTableData(Class<?> tableClass) {
		return tableMap.get(tableClass);
	}
	
	public void registerTableData(TableData table) throws DataIntegrityException {
		if (table == null) return;
		
		if (table.tableClass == null) {
			throw new DataIntegrityException("Table: Storage class in null"); 
		}
		
		if (table.fields == null) {
			throw new DataIntegrityException("Table: No fields are defined"); 
		}

		if (table.fields.length == 0) {
			throw new DataIntegrityException("Table: No fields are defined"); 
		}

		if (table.primaryKey == null) {
			throw new DataIntegrityException("Table: primary key is not defined"); 
		}

		int columnIdx = 0;
		HashSet<FieldData> ff = new HashSet<FieldData>();
		
		for (FieldData fd: table.fields) {
			if (fd.columnName == null) {
				throw new DataIntegrityException("Field: Column name is null"); 
			}
			
			if (fd.valueAccessor == null) {
				throw new DataIntegrityException("Field: Value accessor is null"); 
			}
			
			if (fd.valueAccessor.getStorageClass().isAssignableFrom(table.tableClass)) {
				throw new DataIntegrityException("Field: invalid storage class"); 
			}
			
			fd.columnIdx = columnIdx;
			columnIdx++;
			ff.add(fd);
		}
		
		if (!ff.contains(table.primaryKey)) {
			throw new DataIntegrityException("Primary Key: should be defined in field list."); 
		}
		
		if (table.primaryKey.dataType != FieldDataType.LONG) {
			throw new DataIntegrityException("Primary Key: should have LONG data type."); 
		}
		
		if (table.indice != null) {
			for (IndexData index: table.indice) {
				if (index == null) {
					throw new DataIntegrityException("Index: null"); 
				}
				if (index.fields == null) {
					throw new DataIntegrityException("Index: no fields are defined"); 
				}
				if (index.fields.length == 0) {
					throw new DataIntegrityException("Index: no fields are defined"); 
				}
				for (FieldData field: index.fields) {
					if (!ff.contains(field)) {
						throw new DataIntegrityException("Index: field should be defined in field list."); 
					}
				}
			}
		}
		
		table.tableNo = tableMap.size();
		tableMap.put(table.tableClass, table);
	}
	
	final Map<Class<?>, List<TableLinkData>> childMap = new HashMap<Class<?>, List<TableLinkData>>();
	
	/**
	 * Contains parent/child relationship
	 * Key: child
	 * Value: parent
	 */
	final Map<Class<?>, Class<?>> parentMap = new HashMap<Class<?>, Class<?>>(); 

	public List<TableLinkData> getChildren(Class<?> clazz) {
		return childMap.get(clazz);
	}
	
	public Class<?> getParentClass(Class<?> clazz) {
		return parentMap.get(clazz);
	}
	
	public void registerTableLink(TableLinkData linkData) throws DataIntegrityException {
		if (linkData == null) return;
		if (linkData.parentClass == null) {
			throw new DataIntegrityException("Table link: Parent class is null"); 
		}
		if (linkData.childClass == null) {
			throw new DataIntegrityException("Table link: Child class is null"); 
		}
		if (linkData.valueAccessor == null) {
			throw new DataIntegrityException("Table link: Parent value accessor is null"); 
		}
		if (linkData.foreignKeyField == null) {
			throw new DataIntegrityException("Table link: Foreign key field is null"); 
		}
		if (!tableMap.containsKey(linkData.parentClass)) {
			throw new DataIntegrityException(String.format("Table link: Table class %s is not registered", linkData.parentClass.getName())); 
		}
		if (!tableMap.containsKey(linkData.childClass)) {
			throw new DataIntegrityException(String.format("Table link: Table class %s is not registered", linkData.childClass.getName())); 
		}
		if (linkData.valueAccessor.getStorageClass().isAssignableFrom(linkData.parentClass)) {
			throw new DataIntegrityException("Table link: Parent value accessor: invalid storage class"); 
		}
		if (linkData.foreignKeyField.valueAccessor.getStorageClass().isAssignableFrom(linkData.childClass)) {
			throw new DataIntegrityException("Table link: Foreign key value accessor: invalid storage class"); 
		}
		if (parentMap.containsKey(linkData.childClass)) {
			throw new DataIntegrityException("Table link: Child link exists"); 
		}
		
		List<TableLinkData> list = childMap.get(linkData.parentClass);
		if (list == null) {
			list = new ArrayList<TableLinkData>();
			childMap.put(linkData.parentClass, list);
		}
		list.add(linkData);
		parentMap.put(linkData.childClass, linkData.parentClass);
	}
	
	final static class TableRelation {
		final Class<?> parent;
		final Class<?> child;
		public TableRelation(Class<?> parent, Class<?> child) {
			this.parent = parent;
			this.child = child;
		}
		
		@Override
		public int hashCode() {
			return (parent != null ? parent.hashCode() : 0) | (child != null ? child.hashCode() : 0);
		}
		
		@Override
		public boolean equals(Object object) {
			if (object == null) return false; 
			if (object.getClass() == this.getClass()) {
				return ((TableRelation) object).parent == parent && ((TableRelation) object).child == child;
			} 
			return false;
		}
	}
	
	final Map<TableRelation, TableQueryBuilder> relationBuilders = new HashMap<TableRelation, TableQueryBuilder>();
	public TableQueryBuilder getRelationQueryBuilder(TableRelation relation) {
		TableQueryBuilder result = relationBuilders.get(relation);
		if (result == null) {
			TableData tableData = getTableData(relation.child);
			if (tableData == null) return null;
			Class<?> current = relation.child;
			ArrayList<TableData> pathToId = new ArrayList<TableData>();
			while (current != null) {
				Class<?> parent = getParentClass(current);
				if (parent == null) return null;
				TableData parentData = getTableData(parent);
				if (parentData == null) return null;
				pathToId.add(parentData);
				if (parent == relation.parent) break;
				current = parent;
			}
			result = new TableQueryBuilder(tableData, pathToId.toArray(new TableData[0]));
		}
		return result;
	} 
	
}
