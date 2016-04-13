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

package org.db.gora.schema;

import org.db.gora.ChildDataType;
import org.db.gora.ChildTableData;
import org.db.gora.DataIntegrityException;
import org.db.gora.FieldData;
import org.db.gora.FieldDataType;
import org.db.gora.IndexData;
import org.db.gora.SqlSchema;
import org.db.gora.TableData;
import org.db.gora.TableLinkData;
import org.db.gora.accessors.DoubleFieldAccessor;
import org.db.gora.accessors.DoublePropertyAccessor;
import org.db.gora.accessors.GenericFieldAccessor;
import org.db.gora.accessors.GenericPropertyAccessor;
import org.db.gora.accessors.IntFieldAccessor;
import org.db.gora.accessors.IntPropertyAccessor;
import org.db.gora.accessors.ListFieldChildAccessor;
import org.db.gora.accessors.ListMethodChildAccessor;
import org.db.gora.accessors.SetFieldChildAccessor;
import org.db.gora.accessors.SetMethodChildAccessor;
import org.db.gora.accessors.SimpleFieldChildAccessor;
import org.db.gora.accessors.SimpleMethodChildAccessor;
import org.db.gora.accessors.StringFieldAccessor;
import org.db.gora.accessors.StringPropertyAccessor;

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

/**
 * Extracts database information from the class annotations and
 * registers it with {@link org.db.gora.SqlSchema}
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */
public class SchemaBuilder {

	/**
	 * Extracts database information and registers it with database schema.
	 *
	 * @param clazz Java class containing sql schema annotation
	 * @param schema {@link org.db.gora.SqlSchema} instance
	 * @throws DataIntegrityException
	 */
	public static void registerEntity(Class<?> clazz, SqlSchema schema) throws DataIntegrityException {
		if (clazz == null || schema == null) return;

		List<TableData> tables = new ArrayList<>();
		List<ChildTableData> children = new ArrayList<>();
		List<TableLinkData> links = new ArrayList<>();

		extractSchema(clazz, tables, children, links);

		for (TableData t: tables) {
			schema.registerTableData(t);
		}
		for (ChildTableData c: children) {
			schema.registerChildTable(c);
		}
		for (TableLinkData l: links) {
			schema.registerEntityLink(l);
		}
	}

	static void extractSchema(Class<?> clazz, List<TableData> tables, List<ChildTableData> children, List<TableLinkData> links)
			throws DataIntegrityException {

		SchemaBuilder.ClassInfo classInfo = extractClassInfo(clazz);
		TableData t = createTableData(classInfo);
		tables.add(t);
		List<ChildTableData> c = createChildTableData(classInfo);
		children.addAll(c);
		List<TableLinkData> l = createTableLinkData(classInfo, t);
		links.addAll(l);

		for (ChildTableData child: c) {
			for (Class<?> childClazz: child.children) {
				extractSchema(childClazz, tables, children, links);
			}
		}
	}

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
			if (clazz.isEnum()) return FieldDataType.STRING;
		}

		throw new DataIntegrityException(String.format("Unsupported field type: %s", clazz.getName()));
	}

	static TableData createTableData(ClassInfo classInfo) throws DataIntegrityException {
		SqlTable table = classInfo.clazz.getAnnotation(SqlTable.class);
		if (table == null) {
			throw new DataIntegrityException(
					String.format("SqlTable annotation is not defined for %s class", classInfo.clazz.getName()));
		}

		TableData tableData = new TableData();
		tableData.tableName = table.name();
		tableData.tableClass = classInfo.clazz;

		ArrayList<FieldData> fields = new ArrayList<>();
		ArrayList<IndexData> indices = new ArrayList<>();

		for (Field field: classInfo.fields) {
			SqlColumn column = field.getAnnotation(SqlColumn.class);
			if (column != null) {
				FieldData fd = new FieldData();
				fd.columnName = column.name();
				fd.fieldName = field.getName();
				fd.dataType = resolveSimpleDataType(field.getType());
				fd.nullable = column.nullable();
				if (column.getter().length() > 0 && column.setter().length() > 0) {
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
					getter.setAccessible(true);
					setter.setAccessible(true);
					switch (fd.dataType) {
						case INT:
							fd.valueAccessor = new IntPropertyAccessor(getter, setter);
							break;
						case DOUBLE:
							fd.valueAccessor = new DoublePropertyAccessor(getter, setter);
							break;
						case STRING:
							fd.valueAccessor = new StringPropertyAccessor(getter, setter);
							break;
						default:
							fd.valueAccessor = new GenericPropertyAccessor(getter, setter);
							break;
					}
				} else {
					field.setAccessible(true);
					switch (fd.dataType) {
						case INT:
							fd.valueAccessor = new IntFieldAccessor(field);
							break;
						case DOUBLE:
							fd.valueAccessor = new DoubleFieldAccessor(field);
							break;
						case STRING:
							fd.valueAccessor = new StringFieldAccessor(field);
							break;
						default:
							fd.valueAccessor = new GenericFieldAccessor(field);
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

	static List<ChildTableData> createChildTableData(ClassInfo classInfo) throws DataIntegrityException {
		List<ChildTableData> result = new ArrayList<>();

		for (Field field: classInfo.fields) {
			SqlChild child = field.getAnnotation(SqlChild.class);
			if (child != null) {

				ChildTableData tld = new ChildTableData();
				tld.parent = classInfo.clazz;

				Class<?> clazz = field.getType();
				if (List.class.isAssignableFrom(clazz)) {
					tld.linkType = ChildDataType.LIST;
				}
				else if (Set.class.isAssignableFrom(clazz)) {
					tld.linkType = ChildDataType.SET;
				} else {
					tld.linkType = ChildDataType.SINGLE;
				}

				Class<?>[] childClasses = child.classes();
				if (childClasses == null) {
					childClasses = new Class<?>[0];
				}

				if (childClasses.length == 0) {
					if (tld.linkType == ChildDataType.LIST || tld.linkType == ChildDataType.SET) {
						ParameterizedType tt = (ParameterizedType) field.getGenericType();
						Type t = tt.getActualTypeArguments()[0];
						if (Class.class.isAssignableFrom(t.getClass())) {
							childClasses = new Class<?>[] { (Class<?>) t };
						}
					} else {
						childClasses = new Class<?>[] { clazz };
					}
				}
				if (childClasses.length == 0) {
					throw new DataIntegrityException(
							String.format("Cannot resolve child storage class: %s.%s",
									field.getDeclaringClass().getName(), field.getName()));
				}
				if (child.getter().length() > 0) {
					Method getter = classInfo.methods.get(child.getter());
					if (getter == null) {
						throw new DataIntegrityException(
								String.format("Cannot find a method %s in class: %s",
										child.getter(), classInfo.clazz.getName()));
					}
					Method setter = null;
					if (child.setter().length() > 0) {
						setter = classInfo.methods.get(child.setter());
					}
					switch(tld.linkType) {
						case LIST:
							tld.valueAccessor = new ListMethodChildAccessor(getter);
							break;
						case SET:
							tld.valueAccessor = new SetMethodChildAccessor(getter);
							break;
						case SINGLE:
							if (setter == null) {
								throw new DataIntegrityException(
										String.format("Cannot find a setter method for child %s in class: %s",
												field.getName(), classInfo.clazz.getName()));
							}
							tld.valueAccessor = new SimpleMethodChildAccessor(getter, setter);
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
							tld.valueAccessor = new ListFieldChildAccessor(field);
							break;
						case SET:
							tld.valueAccessor = new SetFieldChildAccessor(field);
							break;
						case SINGLE:
							tld.valueAccessor = new SimpleFieldChildAccessor(field);
							break;
					}
				}

				tld.children = childClasses;
				result.add(tld);
			}
		}
		return result;
	}

	static class ClassInfo {
		Class<?> clazz;
		ArrayList<Field> fields = new ArrayList<>();
		Map<String, Method> methods = new TreeMap<>();
	}

	static ClassInfo extractClassInfo(Class<?> clazz) {
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

	static List<TableLinkData> createTableLinkData(ClassInfo classInfo, TableData tableData)
			throws DataIntegrityException
	{
		if (classInfo.clazz != tableData.tableClass) {
			throw new DataIntegrityException("extractLinkData: invalid parameters");
		}

		List<TableLinkData> result = new ArrayList<>();
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
