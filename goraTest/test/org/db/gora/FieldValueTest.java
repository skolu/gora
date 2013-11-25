package org.db.gora;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


import org.db.gora.ChildValueAccess;
import org.db.gora.DataIntegrityException;
import org.db.gora.SqliteSchema;
import org.db.gora.TableData;
import org.db.gora.ValueAccess;
import org.db.gora.schema.Customer;
import org.db.gora.schema.Inventory;
import org.db.gora.schema.Invoice;
import org.db.gora.schema.SchemaBuilder;
import org.db.gora.schema.SchemaBuilder.ChildInfo;
import org.db.gora.schema.SchemaBuilder.ClassInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FieldValueTest {
	SqliteSchema schema;
	
	@Before
	public void setUp() throws DataIntegrityException {
		schema = createSchema();
	}
	
	@Test
	public void testFieldValues() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field f = FieldAccess.class.getDeclaredField("intField");
		FieldAccess fa = new FieldAccess();
		
		ValueAccess.ClassFieldValueAccess fv = new ValueAccess.ClassFieldValueAccess(f);
		int declared = 100;
		fv.setValue(Integer.valueOf(declared), fa);
		int expected = (Integer)fv.getValue(fa);
		Assert.assertEquals(expected, declared);
	}
	
	@Test
	public void testChildValues() throws Exception {
		Field f = FieldAccess.class.getDeclaredField("lst");
		FieldAccess fa = new FieldAccess();
		FieldAccessChild fac = new FieldAccessChild();

		ChildValueAccess.ListFieldChildValue lcv = new ChildValueAccess.ListFieldChildValue(f);
		lcv.appendChild(fac, fa);
		
		Assert.assertNotNull(fa.lst);
		Assert.assertEquals(1, fa.lst.size());
	}
	
	@Test
	public void testQueryBuilder() throws DataIntegrityException {
		TableQueryBuilder builder = schema.getQueryBuilder(Invoice.class);
		System.out.print(getTableSchema(builder.tableData));
		System.out.println(builder.getSelectByIdQuery());
		TableQueryBuilder.LinkedQueryBuilder lb = schema.getLinkedQueryBuilder(Invoice.InvoiceItem.class, Invoice.class);
		System.out.println(lb.getSelectByIdQuery());

		lb = schema.getLinkedQueryBuilder(Invoice.InvoiceItemAttribute.class, Invoice.class);
		System.out.println(lb.getSelectByIdQuery());

		lb = schema.getLinkedQueryBuilder(Invoice.InvoicePayment.class, Invoice.class);
		System.out.println(lb.getSelectByIdQuery());
	}
	
	private void createChildren(SqliteSchema schema, ClassInfo classInfo) throws DataIntegrityException {
		List<ChildInfo> children = SchemaBuilder.extractChildInfo(classInfo);
		for (ChildInfo child: children) {
			schema.registerTableData(child.childData);
			schema.registerChildTable(child.childLink);
			
			createChildren(schema, child.childClassInfo);
		}
	}
	
	private String getSqliteColumnType(FieldDataType dataType) {
		switch (dataType) {
		case BOOLEAN:
		case BYTE:
		case SHORT:
		case INT:
		case LONG:
		case DATE:
			return "INTEGER";
			
		case FLOAT:
		case DOUBLE:
			return "REAL";
			
		case STRING:
			return "TEXT";
			
		case BYTEARRAY:
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
			if (id.isUnique) {
				builder.append("CREATE UNIQUE INDEX ");
			} else {
				builder.append("CREATE INDEX ");
			}
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
	public SqliteSchema createSchema() throws DataIntegrityException {
		SqliteSchema schema = new SqliteSchema();
		ClassInfo classInfo = SchemaBuilder.extractClassInfo(Invoice.class);
		TableData table = SchemaBuilder.createTableData(classInfo);

		Assert.assertNotNull(table);
		
		schema.registerTableData(table);

		List<TableLinkData> linkData = SchemaBuilder.extractLinkData(classInfo, table);
		for (TableLinkData link: linkData) {
			schema.registerEntityLink(link); 
		}
		
		createChildren(schema, classInfo);
		
		Assert.assertNotNull(schema.getTableData(Invoice.class));
		Assert.assertNotNull(schema.getTableData(Invoice.InvoiceItem.class));
		Assert.assertNotNull(schema.getTableData(Invoice.InvoicePayment.class));

		classInfo = SchemaBuilder.extractClassInfo(Inventory.class);
		table = SchemaBuilder.createTableData(classInfo);
	
		classInfo = SchemaBuilder.extractClassInfo(Customer.class);
		table = SchemaBuilder.createTableData(classInfo);
		return schema;
	}
	
	public static class FieldAccess {
		public int intField;
		public ArrayList<? extends FieldAccessChild> lst;
	}
	public static class FieldAccessChild {
	}

}
