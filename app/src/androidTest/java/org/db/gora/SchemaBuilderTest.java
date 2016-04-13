package org.db.gora;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.db.gora.schema.Customer;
import org.db.gora.schema.Inventory;
import org.db.gora.schema.Invoice;
import org.db.gora.schema.InvoiceCashPayment;
import org.db.gora.schema.InvoiceCreditPayment;
import org.db.gora.schema.InvoiceCustomer;
import org.db.gora.schema.SchemaUtils;

import java.util.List;

public class SchemaBuilderTest extends TestCase {
    SqlSchema mSchema;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mSchema = SchemaUtils.getSchema();
    }

    public void testSchemaExtract() {
        Assert.assertNotNull(mSchema);

        TableData invoiceData = mSchema.getTableData(Invoice.class);
        Assert.assertNotNull(invoiceData);

        List<ChildTableData> childrenData = mSchema.getChildren(Invoice.class);
        Assert.assertNotNull(childrenData);
        Assert.assertEquals(childrenData.size(), 3);
        for (ChildTableData child: childrenData) {
            if (child.children.length == 1) {
                Assert.assertTrue (child.children[0] == Invoice.InvoiceItem.class || child.children[0] == InvoiceCustomer.class);
            }
            else if (child.children.length == 2) {
                Assert.assertTrue (child.children[0] == InvoiceCashPayment.class || child.children[1] == InvoiceCreditPayment.class);
            }
        }

        childrenData = mSchema.getChildren(Invoice.InvoiceItem.class);
        Assert.assertNotNull(childrenData);
        Assert.assertEquals(childrenData.size(), 1);
        for (ChildTableData child: childrenData) {
            if (child.children.length == 1) {
                Assert.assertTrue(child.children[0] == Invoice.InvoiceItemAttribute.class);
            }
        }

        TableData invnData = mSchema.getTableData(Inventory.class);
        Assert.assertNotNull(invnData);

        TableData custData = mSchema.getTableData(Customer.class);
        Assert.assertNotNull(custData);

        List<TableLinkData> il = mSchema.getDetailLinks(Inventory.class);
        Assert.assertNotNull(il);
        Assert.assertEquals(il.size(), 1);
        Assert.assertEquals(il.get(0).detailClass, Invoice.InvoiceItem.class);

        il = mSchema.getDetailLinks(Customer.class);
        Assert.assertNotNull(il);
        Assert.assertEquals(il.size(), 1);
        Assert.assertEquals(il.get(0).detailClass, Invoice.class);
    }
}
