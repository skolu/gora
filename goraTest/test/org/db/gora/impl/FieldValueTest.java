package org.db.gora.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;

import junit.framework.Assert;

import org.db.gora.schema.Invoice;
import org.db.gora.schema.SchemaBuilder;
import org.db.gora.schema.SchemaBuilder.ClassInfo;
import org.db.gora.sqlite.ChildValueAccess;
import org.db.gora.sqlite.DataIntegrityException;
import org.db.gora.sqlite.TableData;
import org.db.gora.sqlite.ValueAccess;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FieldValueTest {
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

		ChildValueAccess.ListFieldChildValueAccess lcv = new ChildValueAccess.ListFieldChildValueAccess(f);
		lcv.appendChild(fac, fa);
		
		Assert.assertNotNull(fa.lst);
		Assert.assertEquals(1, fa.lst.size());
	}
	
	@Test 
	public void testClassExtract() throws DataIntegrityException {
		ClassInfo classInfo = SchemaBuilder.extractClassInfo(Invoice.class);
		TableData table = SchemaBuilder.createTableData(classInfo);
		
		Assert.assertNotNull(table);
	}
	
	public static class FieldAccess {
		public int intField;
		public ArrayList<? extends FieldAccessChild> lst;
	}
	public static class FieldAccessChild {
	}

}
