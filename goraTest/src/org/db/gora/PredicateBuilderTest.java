package org.db.gora;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.db.gora.schema.Invoice;
import org.db.gora.schema.SchemaUtils;

public class PredicateBuilderTest extends TestCase {
    SQLiteSchema schema;

    @Override
    public void setUp() throws DataIntegrityException {
        schema = SchemaUtils.getSchema();
    }

    public void testInvoicePredicateBuilder() {

        PredicateBuilder pb = new PredicateBuilder(schema.getTableData(Invoice.class));
        Assert.assertNotNull(pb);

        PredicateBuilder.WhereClause wc = pb.where();
        wc.eq("name", "I001").exclude();
        String actual = wc.getWhereClause();
        actual = actual.replaceAll("^\\(+", "");
        actual = actual.replaceAll("\\)+$", "");
        Assert.assertEquals(actual, "name <> 'I001'");

        wc.clear();

        wc.range("modified", 123456789, 987654321);
        actual = wc.getWhereClause();
        actual = actual.replaceAll("^\\(+", "");
        actual = actual.replaceAll("\\)+$", "");
        Assert.assertEquals(actual, "modified BETWEEN 123456789 AND 987654321");
    }
}
