package org.db.gora;

import java.lang.reflect.Field;
import java.util.ArrayList;


import junit.framework.Assert;
import org.db.gora.accessors.IntFieldAccessor;
import org.db.gora.accessors.ListFieldChildAccessor;
import org.db.gora.schema.Invoice;

import junit.framework.TestCase;
import org.db.gora.schema.SchemaUtils;

public class FieldValueTest extends TestCase {
	SQLiteSchema schema;

	@Override
	public void setUp() throws DataIntegrityException {
		schema = SchemaUtils.getSchema();
	}
	
	public void testFieldValues() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, DataAccessException {
		Field f = FieldAccess.class.getDeclaredField("intField");
		FieldAccess fa = new FieldAccess();
		
		IntFieldAccessor fv = new IntFieldAccessor(f);
		int declared = 100;
		fv.setValue(Integer.valueOf(declared), fa);
		int expected = (Integer)fv.getValue(fa);
		Assert.assertEquals(expected, declared);
	}
	
	public void testChildValues() throws Exception {
		Field f = FieldAccess.class.getDeclaredField("lst");
		FieldAccess fa = new FieldAccess();
		FieldAccessChild fac = new FieldAccessChild();

		ListFieldChildAccessor lcv = new ListFieldChildAccessor(f);
		lcv.appendChild(fac, fa);
		
		Assert.assertNotNull(fa.lst);
		Assert.assertEquals(1, fa.lst.size());
	}
	
	public void testQueryBuilder() throws DataIntegrityException {

		TableQueryBuilder builder = schema.getQueryBuilder(Invoice.class);
		System.out.print(getTableSchema(builder.tableData));
		System.out.println(builder.getSelectByIdQuery());
		//TableQueryBuilder.LinkedQueryBuilder lb = schema.getLinkedQueryBuilder(Invoice.InvoiceItem.class, Invoice.class);
		//System.out.println(lb.getSelectByIdQuery());

		//lb = schema.getLinkedQueryBuilder(Invoice.InvoiceItemAttribute.class, Invoice.class);
		//System.out.println(lb.getSelectByIdQuery());

		//lb = schema.getLinkedQueryBuilder(Invoice.InvoicePayment.class, Invoice.class);
		//System.out.println(lb.getSelectByIdQuery());
	}
	

	private String getSqliteColumnType(FieldDataType dataType) {
		switch (dataType) {
		case BOOLEAN:
		case INT:
		case LONG:
		case DATE:
			return "INTEGER";
			
		case DOUBLE:
			return "REAL";
			
		case STRING:
			return "TEXT";
			
		case BYTE_ARRAY:
			return "BLOB";
		}
		return "TEXT";
	}
	private String getTableSchema(TableData table) {
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("CREATE TABLE %s (\n", table.tableName));
		for (int i = 0; i < table.fields.length; ++i) {
			FieldData df = table.fields[i];
			builder.append(String.format("\t%s\t%s,\n", df.columnName, getSqliteColumnType(df.dataType)));
		}
		builder.append(String.format("\tPRIMARY KEY(%s)\n", table.primaryKey.columnName));
		builder.append(");\n");
		for (IndexData id: table.indice) {
            builder.append(String.format("CREATE %s INDEX ", id.isUnique ? "UNIQUE" : ""));
			for (int i = 0; i < id.fields.length; i++) {
				if (i > 0) {
					builder.append("_");
				}
				builder.append(id.fields[i].columnName.toUpperCase());
			}

			builder.append(String.format("_IDX ON %s (", table.tableName));
			for (int i = 0; i < id.fields.length; i++) {
				if (i > 0) {
					builder.append(", ");
				}
				builder.append(id.fields[i].columnName);
			}
			builder.append(");\n");
		}
		return builder.toString();
	}

	public static class FieldAccess {
		public int intField;
		public ArrayList<? extends FieldAccessChild> lst;
	}
	public static class FieldAccessChild {
	}

}
