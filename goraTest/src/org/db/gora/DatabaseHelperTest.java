package org.db.gora;

import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import junit.framework.Assert;
import org.db.gora.schema.*;

import java.util.ArrayList;
import java.util.Date;

public class DatabaseHelperTest extends AndroidTestCase {
    SQLiteSchema schema;
    DatabaseHelper helper;
    SQLiteDatabase db;

    @Override
    public void setUp() throws DataIntegrityException {
        schema = SchemaUtils.getSchema();
        helper = new DatabaseHelper(getContext(), null, schema);
        db = helper.getWritableDatabase();
    }

    public void testDbCreate() throws DataAccessException {
        Assert.assertNotNull(db);
    }

    public void testCustomer() throws DataAccessException, DataIntegrityException {
        SQLiteManager sm = new SQLiteManager(db, schema);

        Customer customer = new Customer();
        customer.name = "Sergey Kolupaev";
        customer.firstName = "Sergey";
        customer.lastName = "Kolupaev";

        sm.write(customer);
        Assert.assertEquals(customer.getId(), 1L);

        customer = sm.read(Customer.class, 1L);
        Assert.assertEquals(customer.name, "Sergey Kolupaev");
        Assert.assertEquals(customer.type, EntityType.Regular);

        customer.firstName = "Сергей";
        customer.lastName = "Колупаев";

        sm.write(customer);
        Assert.assertEquals(customer.getId(), 1L);
        customer = sm.read(Customer.class, 1L);
        Assert.assertEquals(customer.firstName, "Сергей");

        PredicateBuilder builder = sm.getPredicateBuilder(Customer.class);
        PredicateBuilder.WhereClause where = builder.where();
        where.eq("name", "Sergey Kolupaev");
        String whereClause = where.getWhereClause();
        Assert.assertNotNull(whereClause);

        long[] ids = sm.queryIds(Customer.class, whereClause, null, null);
        Assert.assertNotNull(ids);
        Assert.assertEquals(ids.length, 1);
        Assert.assertEquals(ids[0], 1L);

        ClosableIterator<Customer> itr = sm.query(Customer.class, whereClause, null);
        Assert.assertNotNull(itr);
        int cnt = 0;
        while (itr.hasNext()) {
            Customer c = itr.next();
            Assert.assertEquals(c.firstName, "Сергей");
            ++cnt;
        }
        Assert.assertEquals(cnt, 1);


        where = builder.where();
        where.gt("modified", new Date(0L)).and().eq("type", EntityType.Regular) ;
        whereClause = where.getWhereClause();

        DataManager.FieldCursor cursor = sm.queryFields(Customer.class, whereClause, null, "name");
        while (!cursor.eof()) {
            long id = cursor.getId();
            String name = (String) cursor.getFieldValue(0);
            Assert.assertEquals(name, "Sergey Kolupaev");
            cursor.next();
        }

        sm.delete(Customer.class, 1L);
        customer = sm.read(Customer.class, 1L);
        Assert.assertNull(customer);
    }

    public void testInvoice() throws DataAccessException, DataIntegrityException {
        SQLiteManager sm = new SQLiteManager(db, schema);

        Customer customer = new Customer();
        customer.name = "Sergey Kolupaev";
        customer.firstName = "Sergey";
        customer.lastName = "Kolupaev";

        sm.write(customer);

        Inventory invn1 = new Inventory();
        invn1.itemNo = 10;
        invn1.name = "Item 1";
        invn1.desc = "Товар 1";
        invn1.price = 9.99;
        invn1.taxable = true;
        invn1.image = new byte[] {0,0,0,0};

        sm.write(invn1);
        long invn1_id = invn1.getId();

        Inventory invn2 = new Inventory();
        invn2.itemNo = 11;
        invn2.name = "Item 2";
        invn2.desc = "Товар 2";
        invn2.price = 19.99;
        invn2.taxable = false;

        sm.write(invn2);

        Invoice invoice = new Invoice();
        invoice.customer = new Invoice.InvoiceCustomer(customer);
        invoice.items = new ArrayList<Invoice.InvoiceItem>();
        Invoice.InvoiceItem invc_item = new Invoice.InvoiceItem(invn1);
        invc_item.qty = 1.;
        invoice.items.add(invc_item);

        invc_item = new Invoice.InvoiceItem(invn2);
        invc_item.qty = 2.;
        invoice.items.add(invc_item);

        Invoice.InvoicePayment payment = new Invoice.InvoicePayment();
        payment.amount = 100;
        invoice.getPayments().add(payment);

        sm.write(invoice);
        Assert.assertEquals(invoice.getId(), 1L);

        invoice = sm.read(Invoice.class, 1L);
        Assert.assertNotNull(invoice);
        Assert.assertNotNull(invoice.customer);
        Assert.assertNotNull(invoice.items);
        Assert.assertEquals(invoice.items.size(), 2);
        Assert.assertFalse(invoice.items.get(0).getId() == invoice.items.get(1).getId());

        invoice.items.remove(1);
        sm.write(invoice);

        invoice = sm.read(Invoice.class, 1L);
        Assert.assertNotNull(invoice);
        Assert.assertNotNull(invoice.customer);
        Assert.assertNotNull(invoice.items);
        Assert.assertEquals(invoice.items.size(), 1);

        long[] ids = sm.queryLinks(Invoice.class, Customer.class, 1L);
        Assert.assertNotNull(ids);
        Assert.assertEquals(ids.length, 1);
        assertEquals(ids[0], 1);

        ids = sm.queryLinks(Invoice.class, Inventory.class, 1L);
        Assert.assertNotNull(ids);
        Assert.assertEquals(ids.length, 1);
        assertEquals(ids[0], 1);

        sm.delete(Invoice.class, 1L);
        invoice = sm.read(Invoice.class, 1L);
        Assert.assertNull(invoice);


        invn1 = sm.read(Inventory.class, invn1_id);
        Assert.assertNotNull(invn1);
        Assert.assertNotNull(invn1.image);
        Assert.assertEquals(invn1.image.length, 4);
    }



}
