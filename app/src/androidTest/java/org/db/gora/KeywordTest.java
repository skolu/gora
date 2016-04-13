package org.db.gora;

import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import junit.framework.Assert;
import org.db.gora.schema.Inventory;
import org.db.gora.schema.SchemaUtils;

public class KeywordTest extends AndroidTestCase {

    SqlSchema schema;
    DatabaseHelper helper;
    SQLiteDatabase db;

    @Override
    public void setUp() throws DataIntegrityException {
        schema = SchemaUtils.getSchema();
        helper = new DatabaseHelper(getContext(), null, schema);
        db = helper.getWritableDatabase();
    }

    public void testKeywords() throws DataAccessException, DataIntegrityException {
        SqliteManager sm = new SqliteManager(db, schema);

        Inventory invn1 = new Inventory();
        invn1.itemNo = 10;
        invn1.name = "Adidas Running Shoes";
        invn1.desc = "Кроссовки Адидас";
        invn1.price = 9.99;
        invn1.taxable = true;

        sm.write(invn1);

        Inventory invn2 = new Inventory();
        invn2.itemNo = 11;
        invn2.name = "Nike T-Shirt";
        invn2.desc = "Футболка Найк";
        invn2.price = 19.99;
        invn2.taxable = false;

        sm.write(invn2);

        long[] ids = sm.queryKeywords(Inventory.class, "shoe*");
        Assert.assertNotNull(ids);
        Assert.assertEquals(ids.length, 1);
        Assert.assertEquals(ids[0], invn1.getId());

        ids = sm.queryKeywords(Inventory.class, "футбол*");
        Assert.assertNotNull(ids);
        Assert.assertEquals(ids.length, 1);
        Assert.assertEquals(ids[0], invn2.getId());

    }

}
