package org.db.gora;

import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import junit.framework.Assert;
import org.db.gora.schema.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class DatabaseHelperTest extends AndroidTestCase {
    SqlSchema schema;
    DatabaseHelper helper;
    SQLiteDatabase db;

    @Override
    public void setUp() throws DataIntegrityException {
        schema = SchemaUtils.getSchema();

        File f = getContext().getDatabasePath("test.db");
        if (f.exists()) {
            boolean ok = f.delete();
            Assert.assertTrue(ok);
        }

        helper = new DatabaseHelper(getContext(), "test.db", schema);
        db = helper.getWritableDatabase();
    }

    public void testDbCreate() throws DataAccessException {
        Assert.assertNotNull(db);
    }

    public void testDbUpgrade() throws DataAccessException {
        String dbName = "aa.db";
        DatabaseHelper h = new DatabaseHelper(getContext(), dbName, schema);

        File f = getContext().getDatabasePath(dbName);
        if (f.exists()) {
            boolean ok = f.delete();
            Assert.assertTrue(ok);
        }

        SchemaUtils.sDatabaseVersion = 2;
        h = new DatabaseHelper(getContext(), dbName, schema);
        SQLiteDatabase d = h.getWritableDatabase();
        Assert.assertNotNull(d);
    }

    public void testCustomer() throws DataAccessException, DataIntegrityException {
        SqliteManager sm = new SqliteManager(db, schema);

        Customer customer = new Customer();
        customer.name = "Sergey Kolupaev";
        customer.firstName = "Sergey";
        customer.setLastName("Kolupaev");

        sm.write(customer);
        Assert.assertEquals(customer.getId(), 1L);

        customer = sm.read(Customer.class, 1L);
        Assert.assertEquals(customer.name, "Sergey Kolupaev");
        Assert.assertEquals(customer.type, EntityType.Regular);

        customer.firstName = "Сергей";
        customer.setLastName("Колупаев");

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
        SqliteManager sm = new SqliteManager(db, schema);

        Customer customer = new Customer();
        customer.name = "Sergey Kolupaev";
        customer.firstName = "Sergey";
        customer.setLastName("Kolupaev");

        Address addr = new Address();
        addr.address1 = "100 Main st.";
        addr.city = "SimCity";
        addr.state = "AZ";
        addr.zipCode = "55555";
        customer.getAddresses().add(addr);

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
        invoice.name = "INVC:0001";
        invoice.customer = new InvoiceCustomer(customer);
        invoice.customerId = customer.getId();
        invoice.items = new ArrayList<>();
        Invoice.InvoiceItem invc_item = new Invoice.InvoiceItem(invn1);
        invc_item.setQty(1.f);
        invoice.items.add(invc_item);

        invc_item = new Invoice.InvoiceItem(invn2);
        invc_item.setQty(2.f);
        invoice.items.add(invc_item);

        InvoiceCashPayment cash = new InvoiceCashPayment();
        cash.amount = 10.;
        cash.cashTaken = 20.;
        invoice.getPayments().add(cash);

        InvoiceCreditPayment credit = new InvoiceCreditPayment();
        credit.authId = "AUTH1234567890";
        credit.lastFourDigits = "4321";
        credit.nameOnCard = "TEST";
        credit.amount = 20;
        invoice.getPayments().add(credit);

        sm.write(invoice);
        Assert.assertEquals(invoice.getId(), 1L);

        sm.delete(Inventory.class, invn2.getId());

        invoice = sm.read(Invoice.class, 1L);
        Assert.assertNotNull(invoice);
        Assert.assertNotNull(invoice.customer);
        Assert.assertNotNull(invoice.items);
        Assert.assertEquals(invoice.items.size(), 2);
        for (Invoice.InvoiceItem item: invoice.items) {
            switch (item.getName()) {
                case "Item 1":
                    Assert.assertTrue(item.getQty() > 0.99f);
                    Assert.assertTrue(item.getQty() < 1.01f);
                    Assert.assertTrue(item.taxable);
                    Assert.assertEquals(item.invn_id, invn1_id);
                    break;
                case "Item 2":
                    Assert.assertTrue(item.getQty() > 1.99f);
                    Assert.assertTrue(item.getQty() < 2.01f);
                    Assert.assertFalse(item.taxable);
                    Assert.assertEquals(item.invn_id, 0);
                    break;
                default:
                    Assert.assertTrue(false);
                    break;
            }
        }
        Assert.assertFalse(invoice.items.get(0).getId() == invoice.items.get(1).getId());

        Set<InvoicePayment> payments = invoice.getPayments();
        Assert.assertNotNull(payments);
        Assert.assertEquals(payments.size(), 2);
        for (InvoicePayment payment: payments) {
            if (payment.getPaymentType() == PaymentType.Cash) {
                Assert.assertTrue(payment.amount > 9.99);
                Assert.assertTrue(payment.amount < 10.01);
            }
            else if (payment.getPaymentType() == PaymentType.Credit) {
                Assert.assertTrue(payment.amount > 19.99);
                Assert.assertTrue(payment.amount < 20.01);
            } else {
                Assert.assertTrue(false);
            }
        }

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
