package org.db.gora;

import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import junit.framework.Assert;
import org.db.gora.schema.Customer;
import org.db.gora.schema.SchemaUtils;

public class DatabaseHelperTest extends AndroidTestCase {
    SQLiteSchema schema;
    DatabaseHelper helper;

    @Override
    public void setUp() throws DataIntegrityException {
        schema = SchemaUtils.getSchema();
        helper = new DatabaseHelper(getContext(), null, schema);
    }

    public void testDbCreate() throws DataAccessException {
        SQLiteDatabase db = helper.getWritableDatabase();
        Assert.assertNotNull(db);

        SQLiteManager sm = new SQLiteManager(db, schema);

        Customer customer = new Customer();
        customer.name = "Sergey Kolupaev";
        customer.firstName = "Sergey";
        customer.lastName = "Kolupaev";

        sm.write(customer);
        Assert.assertEquals(customer.getId(), 1L);

        customer = sm.read(Customer.class, 1L);
        Assert.assertEquals(customer.name, "Sergey Kolupaev");
    }

}
