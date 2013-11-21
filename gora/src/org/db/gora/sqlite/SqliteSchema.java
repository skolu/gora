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
	
	final Map<Class<?>, TableQueryBuilder> queryBuilders = new HashMap<Class<?>, TableQueryBuilder>();
	TableQueryBuilder getQueryBuilder(Class<?> clazz) {
		TableQueryBuilder result = queryBuilders.get(clazz);
		if (result == null) {
			TableData tableData = getTableData(clazz);
			if (tableData == null) return null;
			result = new TableQueryBuilder(tableData);
			queryBuilders.put(clazz, result);
		}
		return result;
	} 
	
	TableQueryBuilder.LinkedQueryBuilder getLinkedQueryBuilder(Class<?> clazz, Class<?> idClazz)
			throws DataIntegrityException 
	{
		TableQueryBuilder tableBuilder = getQueryBuilder(clazz);
		TableQueryBuilder.LinkedQueryBuilder result = tableBuilder.linkedBuilders.get(clazz);
		if (result == null) {
			Class<?> current = clazz;
			ArrayList<TableData> pathToId = new ArrayList<TableData>();
			while (current != null) {
				Class<?> parent = getParentClass(current);
				if (parent == null) return null;
				TableData parentData = getTableData(parent);
				if (parentData == null) return null;
				pathToId.add(parentData);
				if (parent == idClazz) break;
				current = parent;
			}
			if (current != idClazz) {
				throw new DataIntegrityException(
						String.format("Linked Query Builder: Classes %s and %s are not linked.", 
								clazz.getName(), idClazz.getName())); 
			}
			
			result = tableBuilder.new LinkedQueryBuilder(pathToId.toArray(new TableData[0]));
			tableBuilder.linkedBuilders.put(idClazz, result);
		}
		return result;
	}
}
