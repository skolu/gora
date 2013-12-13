package org.db.gora.schema;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.db.gora.ChildDataType;
import org.db.gora.ChildTableData;
import org.db.gora.ChildValueAccess;
import org.db.gora.DataIntegrityException;
import org.db.gora.FieldData;
import org.db.gora.FieldDataType;
import org.db.gora.IndexData;
import org.db.gora.TableData;
import org.db.gora.TableLinkData;
import org.db.gora.accessors.DoubleFieldValueAccessor;
import org.db.gora.accessors.DoublePropertyValueAccessor;
import org.db.gora.accessors.GenericFieldValueAccessor;
import org.db.gora.accessors.GenericPropertyValueAccessor;
import org.db.gora.accessors.IntFieldValueAccessor;
import org.db.gora.accessors.IntPropertyValueAccessor;

public class SchemaBuilder {

	static FieldDataType resolveSimpleDataType(Class<?> clazz) throws DataIntegrityException {
        if (clazz.isPrimitive()) {
            if (clazz == Byte.TYPE) return FieldDataType.INT;
            if (clazz == Short.TYPE) return FieldDataType.INT;
            if (clazz == Integer.TYPE) return FieldDataType.INT;
            if (clazz == Long.TYPE) return FieldDataType.LONG;
            if (clazz == Float.TYPE) return FieldDataType.DOUBLE;
            if (clazz == Double.TYPE) return FieldDataType.DOUBLE;
            if (clazz == Boolean.TYPE) return FieldDataType.BOOLEAN;
        } else {
            if (clazz == String.class) return FieldDataType.STRING;
            if (clazz == Date.class) return FieldDataType.DATE;
            if (clazz == byte[].class) return FieldDataType.BYTE_ARRAY;
            if (clazz == BigDecimal.class) return FieldDataType.DOUBLE;
        }

        throw new DataIntegrityException(String.format("Unsupported field type: %s", clazz.getName()));
	} 
	
	public static TableData createTableData(ClassInfo classInfo) throws DataIntegrityException {
		SqlTable table = classInfo.clazz.getAnnotation(SqlTable.class);
		if (table == null) {
			throw new DataIntegrityException(
					String.format("SqlTable annotation is not defined for %s class", classInfo.clazz.getName()));
		}
		
		TableData tableData = new TableData();
		tableData.tableName = table.name();
		tableData.tableClass = classInfo.clazz;
		
		ArrayList<FieldData> fields = new ArrayList<FieldData>();
		ArrayList<IndexData> indices = new ArrayList<IndexData>();
		
		for (Field field: classInfo.fields) {
			SqlColumn column = field.getAnnotation(SqlColumn.class);
			if (column != null) {
				FieldData fd = new FieldData();
				fd.columnName = column.name();
                fd.fieldName = field.getName();
				fd.dataType = resolveSimpleDataType(field.getType());
				fd.nullable = column.nullable();
				int modifiers = field.getModifiers();
				if ((modifiers & Modifier.PUBLIC) != 0) {
                    switch (fd.dataType) {
                        case INT:
                            fd.valueAccessor = new IntFieldValueAccessor(field);
                            break;
                        case DOUBLE:
                            fd.valueAccessor = new DoubleFieldValueAccessor(field);
                            break;
                        default:
                            fd.valueAccessor = new GenericFieldValueAccessor(field);
                            break;
                    }
				} else {
					Method getter = classInfo.methods.get(column.getter());
					Method setter = classInfo.methods.get(column.setter());
					if (getter == null || setter == null) {
				        throw new DataIntegrityException(
				        		String.format("Setter or/and getter are not defined for non-public field: %s.%s", 
				        				field.getDeclaringClass().getName(), field.getName()));
					}
					if (getter.getReturnType() == Void.TYPE || getter.getParameterTypes().length != 0) {
				        throw new DataIntegrityException(
				        		String.format("Method %s.%s does not look like getter method", 
				        				getter.getDeclaringClass().getName(), getter.getName()));
					}
					if (setter.getReturnType() != Void.TYPE || setter.getParameterTypes().length != 1) {
				        throw new DataIntegrityException(
				        		String.format("Method %s.%s does not look like setter method", 
				        				setter.getName(), setter.getDeclaringClass().getName()));
					}
					if (getter.getReturnType() != setter.getParameterTypes()[0]) {
				        throw new DataIntegrityException(
				        		String.format("Getter %s.%s and Setter %s.%s use different types",
				        				getter.getDeclaringClass().getName(), getter.getName(),
				        				setter.getDeclaringClass().getName(), setter.getName()));
					}

					fd.dataType = resolveSimpleDataType(getter.getReturnType());
                    switch (fd.dataType) {
                        case INT:
                            fd.valueAccessor = new IntPropertyValueAccessor(getter, setter);
                            break;
                        case DOUBLE:
                            fd.valueAccessor = new DoublePropertyValueAccessor(getter, setter);
                            break;
                        default:
                            fd.valueAccessor = new GenericPropertyValueAccessor(getter, setter);
                            break;
                    }
				}
				fields.add(fd);
				if (column.pk()) {
					tableData.primaryKey = fd;
				} 
				if (column.fk()) {
					tableData.foreignKey = fd;
					
					IndexData id = new IndexData();
					id.isUnique = false;
					id.fields = new FieldData[] {fd};
					indices.add(id);
				}
				else if (column.index()) {
					IndexData id = new IndexData();
					id.isUnique = column.unique();
					id.fields = new FieldData[] {fd};
					indices.add(id);
				}
				else if (field.getAnnotation(SqlLinkedEntity.class) != null) {
					IndexData id = new IndexData();
					id.isUnique = false;
					id.fields = new FieldData[] {fd};
					indices.add(id);
				}
			}
		}
		
		tableData.fields = fields.toArray(new FieldData[fields.size()]);
		tableData.indice = indices.toArray(new IndexData[indices.size()]);
		return tableData;
	}
	
	public static class ChildInfo {
		public ChildTableData childLink;
		public TableData childData;
		public ClassInfo childClassInfo;
	}
	
	public static List<ChildInfo> extractChildInfo(ClassInfo classInfo) throws DataIntegrityException {
		List<ChildInfo> result = new ArrayList<ChildInfo>();
		
		for (Field field: classInfo.fields) {
			SqlChild child = field.getAnnotation(SqlChild.class);
			if (child != null) {
				ChildTableData tld = new ChildTableData();
				tld.parentClass = classInfo.clazz;
				
				Class<?> clazz = field.getType();
				if (List.class.isAssignableFrom(clazz)) {
					tld.linkType = ChildDataType.LIST;
				}
				else if (Set.class.isAssignableFrom(clazz)) {
					tld.linkType = ChildDataType.SET;
				} else {
					tld.linkType = ChildDataType.SINGLE;
				}

				tld.childClass = child.clazz();
				if (tld.childClass == Void.class) {
					if (tld.linkType == ChildDataType.LIST || tld.linkType == ChildDataType.SET) {
						ParameterizedType tt = (ParameterizedType) field.getGenericType();
						Type t = tt.getActualTypeArguments()[0];
						if (Class.class.isAssignableFrom(t.getClass())) {
							tld.childClass = (Class<?>) t;
						}
					} else {
						tld.childClass = clazz;
					}
				}
				if (tld.childClass == Void.class) {
			        throw new DataIntegrityException(
			        		String.format("Cannot resolve child storage class: %s.%s",
			        				field.getDeclaringClass().getName(), field.getName()));
				}
				if (child.getter().length() > 0) {
					Method method = classInfo.methods.get(child.getter());
					if (method == null) {
				        throw new DataIntegrityException(
				        		String.format("Cannot find a method %s in class: %s",
				        				child.getter(), classInfo.clazz.getName()));
					}
					switch(tld.linkType) {
					case LIST:
						tld.valueAccessor = new ChildValueAccess.ListMethodChildValue(method);
						break;
					case SET:
						tld.valueAccessor = new ChildValueAccess.SetMethodChildValue(method);
						break;
					case SINGLE:
						tld.valueAccessor = new ChildValueAccess.SimpleMethodChildValue(method);
						break;
					}
					
				} else {
					int modifiers = field.getModifiers();
					if ((modifiers & Modifier.PUBLIC) == 0) {
				        throw new DataIntegrityException(
				        		String.format("Field %s is not public in class: %s",
				        				field.getName(), classInfo.clazz.getName()));
					}
					switch(tld.linkType) {
					case LIST:
						tld.valueAccessor = new ChildValueAccess.ListFieldChildValue(field);
						break;
					case SET:
						tld.valueAccessor = new ChildValueAccess.SetFieldChildValue(field);
						break;
					case SINGLE:
						tld.valueAccessor = new ChildValueAccess.SimpleFieldChildValue(field);
						break;
					}
				}
				
				ClassInfo childClassInfo = extractClassInfo(tld.childClass);
				TableData childData = createTableData(childClassInfo);
				if (childData.foreignKey == null) {
			        throw new DataIntegrityException(
			        		String.format("Foreign key field is not defined in class: %s",
			        				tld.childClass.getName()));
				} 
				tld.foreignKeyField = childData.foreignKey;
				ChildInfo childInfo = new ChildInfo();
				childInfo.childLink = tld;
				childInfo.childData = childData;
				childInfo.childClassInfo = childClassInfo;
				
				result.add(childInfo);
			}
		}
		
		return result;
	}
	
	public static class ClassInfo {
		Class<?> clazz;
		ArrayList<Field> fields = new ArrayList<Field>();
		Map<String, Method> methods = new TreeMap<String, Method>();
	} 

	public static ClassInfo extractClassInfo(Class<?> clazz) {
		ClassInfo classInfo = new ClassInfo();
		classInfo.clazz = clazz;
		
		Class<?> c = clazz;
		while (c != null) {
			Field[] fa = c.getDeclaredFields();
			Method[] ma = c.getDeclaredMethods();
			
			if (fa != null) {
				for (int i = fa.length - 1; i >= 0; --i){
					Field f = fa[i];
					int modifiers = f.getModifiers();
					if ((modifiers & Modifier.STATIC) == 0) {
						classInfo.fields.add(f);
					} 
				}
			}
			
			if (ma != null) {
				for (int i = ma.length - 1; i >= 0; --i) {
					Method m = ma[i];
					int modifiers = m.getModifiers();
					if ((modifiers & Modifier.STATIC) != 0) continue; 
        			if ((modifiers & Modifier.PUBLIC) == 0) continue;

                    Class<?>[] params = m.getParameterTypes();
        			if (m.getReturnType() != Void.TYPE) { //getter
                        if (params.length != 0) continue;
        			} else { //setter
                        if (params.length != 1) continue;
        			}
					
					classInfo.methods.put(m.getName(), m);
				}
			}
			c = c.getSuperclass();
		}
		
		Collections.reverse(classInfo.fields);
		return classInfo;
	} 
		
	public static List<TableLinkData> extractLinkData(ClassInfo classInfo, TableData tableData) 
			throws DataIntegrityException 
	{
		if (classInfo.clazz != tableData.tableClass) {
	        throw new DataIntegrityException("extractLinkData: invalid parameters");
		}
		
		List<TableLinkData> result = new ArrayList<TableLinkData>();
		for (Field field: classInfo.fields) {
			SqlLinkedEntity link = field.getAnnotation(SqlLinkedEntity.class);
			if (link != null) {
				TableLinkData tld = new TableLinkData();
				tld.masterClass = link.entity();
				tld.detailClass = classInfo.clazz;
				tld.whenBroken = link.whenBroken();
				SqlColumn column = field.getAnnotation(SqlColumn.class);
				for (FieldData fd: tableData.fields) {
					if (fd.columnName.equals(column.name())) {
						tld.detailField = fd;
						break;
					}
				}
				if (tld.detailField == null) {
			        throw new DataIntegrityException(
			        		String.format("Field %s.%s is not marked as sql column.",
			        				field.getDeclaringClass().getName(), field.getName()));
				}
				result.add(tld);
			}
		}
		return result;
	}
}
