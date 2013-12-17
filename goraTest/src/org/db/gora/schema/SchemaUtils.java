package org.db.gora.schema;

import org.db.gora.DataIntegrityException;
import org.db.gora.SQLiteSchema;

public class SchemaUtils {

    public static SQLiteSchema getSchema() throws DataIntegrityException {
        if (sSchema == null) {
            sSchema = new SQLiteSchema() {
                @Override
                public int getDatabaseVersion() {
                    return 1;
                }
            };

            SchemaBuilder.registerEntity(Invoice.class, sSchema);
            SchemaBuilder.registerEntity(Customer.class, sSchema);
            SchemaBuilder.registerEntity(Inventory.class, sSchema);
        }
        return sSchema;
    }

    static SQLiteSchema sSchema;
}
