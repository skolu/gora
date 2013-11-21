package org.db.gora.schema;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.db.gora.sqlite.FieldData;
import org.db.gora.sqlite.FieldDataType;
import org.db.gora.sqlite.TableData;
import org.db.gora.sqlite.ValueAccess;

public class SchemaBuilder {
	public static ClassInfo extractClass(Class<?> clazz) {
		ArrayList<Field> fields = new ArrayList<Field>();
		ArrayList<Method> methods = new ArrayList<Method>();
		
		Class<?> c = clazz;
		while (c != null) {
			Field[] fa = c.getDeclaredFields();
			Method[] ma = c.getDeclaredMethods();
			
			if (fa != null) {
				for (Field f: fa) {
					int modifiers = f.getModifiers();
					if ((modifiers & Modifier.STATIC) == 0) {
						fields.add(f);
					} 
				}
			}
			
			if (ma != null) {
				for (Method m: ma) {
					int modifiers = m.getModifiers();
					if ((modifiers & Modifier.STATIC) != 0) continue; 
					
					methods.add(m);
				}
			}
			c = c.getSuperclass();
		}
		
		ArrayList<ExtractedField> publicFields = new ArrayList<ExtractedField>();
		ArrayList<ExtractedProperty> publicProperties = new ArrayList<ExtractedProperty>();
		for (Field f: fields) {
			ExtractedField ef = new ExtractedField();
			ef.kind = FieldKind.NONE;

			Class<?> fc = f.getType();
            String className = fc.getName();
            if (fc.isPrimitive()) {
            	ef.kind = FieldKind.SIMPLE;
                if (fc == Byte.TYPE) {
                	ef.simpleType = FieldDataType.BYTE;
                } else if (fc == Short.TYPE) {
                	ef.simpleType = FieldDataType.SHORT;
                } else if (fc == Integer.TYPE) {
                	ef.simpleType = FieldDataType.INT;
                } else if (fc == Long.TYPE) {
                	ef.simpleType = FieldDataType.LONG;
                } else if (fc == Float.TYPE) {
                	ef.simpleType = FieldDataType.FLOAT;
                } else if (fc == Double.TYPE) {
                	ef.simpleType = FieldDataType.DOUBLE;
                } else if (fc == Boolean.TYPE) {
                	ef.simpleType = FieldDataType.BOOLEAN;
                } else {
                	ef.kind = FieldKind.NONE;
                }
            } else if (className.equals("java.lang.String")) {
            	ef.kind = FieldKind.SIMPLE;
            	ef.simpleType = FieldDataType.STRING;
            } else if (className.equals("java.util.Date")) {
            	ef.kind = FieldKind.SIMPLE;
            	ef.simpleType = FieldDataType.DATE;
            } else if (className.equals("[B")) {
            	ef.kind = FieldKind.SIMPLE;
            	ef.simpleType = FieldDataType.BYTEARRAY;
            } else {
            	ef.kind = FieldKind.CHILD;
            	ef.childType = ChildDataType.SIMPLE;
            	if (List.class.isAssignableFrom(fc)) {
            		ef.childType = ChildDataType.LIST;
            	} else if (Set.class.isAssignableFrom(fc)) {
            		ef.childType = ChildDataType.SET;
            	}
            } 
            if (ef.kind == FieldKind.NONE) continue; 
            
            String fieldName = f.getName();
            ef.field = f;
            
			int modifiers = f.getModifiers();
			if ((modifiers & Modifier.PUBLIC) != 0) {
				publicFields.add(ef);
			} else { // check for getter & setter
				ExtractedProperty ep = new ExtractedProperty();
				ep.kind = ef.kind;
				ep.simpleType = ef.simpleType;
				ep.childType = ef.childType;
				ep.field = ef.field;
				
                String methodName = String.format("%s%C%s", (fc == Boolean.TYPE) ? "is" : "get", fieldName.charAt(0), fieldName.substring(1, fieldName.length()));
                for (Method m: methods) {
                	if (m.getName().equals(methodName)) {
                    	modifiers = m.getModifiers();
            			if ((modifiers & Modifier.PUBLIC) == 0) continue;
            			if (m.getReturnType() != fc) continue;
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length != 0) continue; 
            			ep.getter = m;    	
                        break;
                	}
                }
                if (ep.getter == null) continue;
                
                methodName = String.format("%s%C%s", "set", fieldName.charAt(0), fieldName.substring(1, fieldName.length()));
                for (Method m: methods) {
                	if (m.getName().equals(methodName)) {
                    	modifiers = m.getModifiers();
            			if ((modifiers & Modifier.PUBLIC) == 0) continue;
            			if (m.getReturnType() != Void.TYPE) continue;
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length != 1) continue;
                        if (params[0] != fc) continue;
            			ep.setter = m;    	
                        break;
                	}
                }
                publicProperties.add(ep);
			}
		}
		
		for (ExtractedField f: publicFields) {
			fields.remove(f.field);
		}
		for (ExtractedProperty p: publicProperties) {
			fields.remove(p.field);
			methods.remove(p.getter);
			if (p.setter != null) {
				methods.remove(p.setter);
			}
		}

		TableData tableData = new TableData();
		tableData.tableClass = clazz;

		ArrayList<FieldData> tf = new ArrayList<FieldData>();
		for (ExtractedField ef: publicFields) {
			SqlColumn col = ef.field.getAnnotation(SqlColumn.class);
			if (col != null) {
				switch(ef.kind) {
				case SIMPLE: {
					FieldData fd = new FieldData();
					fd.columnName = col.name();
					fd.dataType = ef.simpleType;
					fd.nullable = col.nullable();
					fd.valueAccessor = new ValueAccess.ClassFieldValueAccess(ef.field);
					tf.add(fd);
					if (col.pk()) {
						tableData.primaryKey = fd;
					}
					if (col.fk()) {
						tableData.foreignKey = fd;
					}
				}
				break;
				}
			} 
		}
		
		
		ClassInfo classInfo = new ClassInfo();
		

		
		return classInfo;
	} 
	
	public static Class<?> getChildClass(ExtractedField fieldInfo) {
		if (fieldInfo.kind != FieldKind.CHILD) return null;
		Class<?> clazz = fieldInfo.field.getType();
		switch (fieldInfo.childType) {
		case SIMPLE:
			return clazz;
		case SET:
		case LIST: {
			if (List.class.isAssignableFrom(clazz) || Set.class.isAssignableFrom(clazz)) {
				ParameterizedType tt = (ParameterizedType) fieldInfo.field.getGenericType();
				if (tt != null) {
					Type t = tt.getActualTypeArguments()[0];
					if (Class.class.isAssignableFrom(t.getClass())) {
						Class<?> result = (Class<?>) t;
						return result;
					}
				}
			}
		}
		break;
		
		default:
			break;
		
		}
		return null;
	}

	public static enum FieldKind {
		NONE,
		SIMPLE,
		CHILD;
	}
	
	public static enum ChildDataType {
		SIMPLE,
		LIST,
		SET;
	}

	public static class ExtractedField {
		public FieldKind kind;
		public FieldDataType simpleType;
		public ChildDataType childType;
		public Field field;
	}

	public static class ExtractedProperty extends ExtractedField {
		public Method getter;
		public Method setter;
	}

	public static class ClassInfo {
		public TableData tableData; 
	} 
}
