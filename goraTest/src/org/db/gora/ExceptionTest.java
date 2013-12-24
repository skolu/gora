package org.db.gora;

import android.test.AndroidTestCase;
import junit.framework.Assert;
import org.db.gora.schema.Customer;
import org.db.gora.schema.SchemaUtils;

public class ExceptionTest extends AndroidTestCase {

    SQLSchema schema;
    DatabaseHelper helper;

    @Override
    public void setUp() throws DataIntegrityException {
        schema = SchemaUtils.getSchema();
        helper = new DatabaseHelper(getContext(), null, schema);
    }

    public void testException() {
        SQLiteManager sm = new SQLiteManager(null, null);
        try {
            sm.write(null);
            Assert.assertTrue(false);
        } catch (DataAccessException e) {
        }
        try {
            sm.write("");
            Assert.assertTrue(false);
        } catch (DataAccessException e) {
        }
        try {
            sm.write(new Customer());
            Assert.assertTrue(false);
        } catch (DataAccessException e) {
        }
        try {
            sm.read(Customer.class, 1L);
            Assert.assertTrue(false);
        } catch (DataAccessException e) {
        }
    }
}
