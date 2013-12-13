package org.db.gora;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public abstract class SQLiteSchema {
	
	final Map<Class<?>, TableData> tableMap = new HashMap<Class<?>, TableData>();
	public TableData getTableData(Class<?> tableClass) {
		return tableMap.get(tableClass);
	}

    public abstract int getDatabaseVersion();

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
			
			if (!fd.valueAccessor.getStorageClass().isAssignableFrom(table.tableClass)) {
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

	final Map<Class<?>, List<TableLinkData>> entityLinkMap = new HashMap<Class<?>, List<TableLinkData>>();
	public void registerEntityLink(TableLinkData link) throws DataIntegrityException {
		if (link == null) return;
		if (link.masterClass == null) {
			throw new DataIntegrityException("Entity link: entity class is null"); 
		}
		if (link.detailClass == null) {
			throw new DataIntegrityException("Entity link: link class is null"); 
		}
		if (link.detailField == null) {
			throw new DataIntegrityException("Entity link: link field is null"); 
		}
		TableData detailData = getTableData(link.detailClass);
		if (detailData == null) {
			throw new DataIntegrityException(String.format("Entity link: Class %s is not registered.", link.detailClass.getName()));
		} else {
            boolean isValid = detailData.ensureIndexExists(link.detailField.columnName);
            if (!isValid) {
                throw new DataIntegrityException(String.format("Entity link: Table %s does not have column %s.", detailData.tableName, link.detailField.columnName));
            }
        }

        List<TableLinkData> links = entityLinkMap.get(link.masterClass);
        if (links == null) {
            links = new ArrayList<TableLinkData>();
            entityLinkMap.put(link.masterClass, links);
        }
        links.add(link);
	}

    public List<TableLinkData> getDetailLinks(Class<?> masterClazz) {
        if (masterClazz != null) {
            return entityLinkMap.get(masterClazz);
        }
        return null;
    }
	
	/**
	 * Contains parent/child relationship
	 * Key: child
	 * Value: parent
	 */
	final Map<Class<?>, Class<?>> parentMap = new HashMap<Class<?>, Class<?>>(); 
	final Map<Class<?>, List<ChildTableData>> childMap = new HashMap<Class<?>, List<ChildTableData>>();

	public List<ChildTableData> getChildren(Class<?> clazz) {
		return childMap.get(clazz);
	}
	
	public Class<?> getParentClass(Class<?> clazz) {
		return parentMap.get(clazz);
	}
	
	public void registerChildTable(ChildTableData linkData) throws DataIntegrityException {
		if (linkData == null) return;
		if (linkData.parentClass == null) {
			throw new DataIntegrityException("Child link: Parent class is null"); 
		}
		if (linkData.childClass == null) {
			throw new DataIntegrityException("Child link: Child class is null"); 
		}
		if (linkData.valueAccessor == null) {
			throw new DataIntegrityException("Child link: Parent value accessor is null"); 
		}
		if (linkData.foreignKeyField == null) {
			throw new DataIntegrityException("Child link: Foreign key field is null"); 
		}
		if (!tableMap.containsKey(linkData.parentClass)) {
			throw new DataIntegrityException(String.format("Child link: Table class %s is not registered", linkData.parentClass.getName())); 
		}
		if (!tableMap.containsKey(linkData.childClass)) {
			throw new DataIntegrityException(String.format("Child link: Table class %s is not registered", linkData.childClass.getName())); 
		}
		if (!linkData.valueAccessor.getStorageClass().isAssignableFrom(linkData.parentClass)) {
			throw new DataIntegrityException("Child link: Parent value accessor: invalid storage class"); 
		}
		if (!linkData.foreignKeyField.valueAccessor.getStorageClass().isAssignableFrom(linkData.childClass)) {
			throw new DataIntegrityException("Child link: Foreign key value accessor: invalid storage class"); 
		}
		if (parentMap.containsKey(linkData.childClass)) {
			throw new DataIntegrityException("Child link: Child link exists"); 
		}
		
		List<ChildTableData> list = childMap.get(linkData.parentClass);
		if (list == null) {
			list = new ArrayList<ChildTableData>();
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
		if (tableBuilder == null) {
			throw new DataIntegrityException(
					String.format("Linked Query Builder: Classes %s is not registered.", 
							clazz.getName())); 
		}
		TableQueryBuilder.LinkedQueryBuilder result = tableBuilder.linkedBuilders.get(clazz);
		if (result != null) {
			Class<?> current = clazz;
			ArrayList<TableData> pathToId = new ArrayList<TableData>();
			while (current != null) {
				Class<?> parent = getParentClass(current);
				if (parent == null) return null;
				TableData parentData = getTableData(parent);
				if (parentData == null) return null;
				pathToId.add(parentData);
				current = parent;
				if (parent == idClazz) break;
			}
			if (current != idClazz) {
				throw new DataIntegrityException(
						String.format("Linked Query Builder: Classes %s and %s are not linked.", 
								clazz.getName(), idClazz.getName())); 
			}
			
			result = tableBuilder.new LinkedQueryBuilder(pathToId.toArray(new TableData[pathToId.size()]));
			tableBuilder.linkedBuilders.put(idClazz, result);
		}
		return result;
	}
}
