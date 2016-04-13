package org.db.gora;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.db.gora.schema.EntityType;
import org.db.gora.schema.Invoice;
import org.db.gora.schema.SchemaUtils;

import java.util.Date;

public class PredicateBuilderTest extends TestCase {
    SqlSchema schema;

    @Override
    public void setUp() throws DataIntegrityException {
        schema = SchemaUtils.getSchema();
    }

    public void testInvoiceWhereBuilder() {

        PredicateBuilder pb = new PredicateBuilder(schema.getTableData(Invoice.class));
        Assert.assertNotNull(pb);

        PredicateBuilder.WhereClause wc = pb.where();

        wc.eq("name", "I001").exclude();
        String actual = wc.getWhereClause();
        actual = actual.replaceAll("^\\(+", "");
        actual = actual.replaceAll("\\)+$", "");
        Assert.assertEquals(actual, "name <> 'I001'");

        wc.clear();

        wc.range("modified", new Date(123456789), new Date(987654321));
        actual = wc.getWhereClause();
        actual = actual.replaceAll("^\\(+", "");
        actual = actual.replaceAll("\\)+$", "");
        Assert.assertEquals(actual, "modified BETWEEN 123456789 AND 987654321");

        wc.clear();
        wc.eq("type", EntityType.Deleted);
        actual = wc.getWhereClause();
        actual = actual.replaceAll("^\\(+", "");
        actual = actual.replaceAll("\\)+$", "");
        Assert.assertEquals(actual, "type = 'Deleted'");

        wc.clear();

        wc.like("name", "INVC:*").or().lt("created", new Date(543216789));
        actual = wc.getWhereClause();
        Assert.assertTrue(actual.contains("name LIKE 'INVC:*'"));
        Assert.assertTrue(actual.contains("created < 543216789"));

        wc.clear();
        wc.set("id", new Long[]{1L,2L,3L,4L});
        actual = wc.getWhereClause();
        Assert.assertEquals(actual, "((id IN (1, 2, 3, 4)))");

        wc.clear();
        wc.eq("type", EntityType.Regular).and().gt("modified", new Date(111111)).or().eq("type", EntityType.Deleted);
        actual = wc.getWhereClause();
        Assert.assertEquals(actual, "((type = 'Regular') AND (modified > 111111)) OR ((type = 'Deleted'))");

    }

    public void testInvoiceOrderByBuilder() throws DataIntegrityException {
        PredicateBuilder pb = new PredicateBuilder(schema.getTableData(Invoice.class));
        Assert.assertNotNull(pb);

        PredicateBuilder.OrderByClause obc = pb.orderBy();

        obc.clear();
        obc.orderBy("modified", false).thenBy("id");

        String actual = obc.getOrderByClause();
        Assert.assertEquals(actual, "modified DESC, id ASC");
    }
}
