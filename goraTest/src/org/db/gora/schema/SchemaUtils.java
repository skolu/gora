package org.db.gora.schema;

import org.db.gora.DataIntegrityException;
import org.db.gora.SQLSchema;

public class SchemaUtils {

    public static int sDatabaseVersion = 1;

    public static SQLSchema getSchema() throws DataIntegrityException {
        if (sSchema == null) {
            sSchema = new SQLSchema() {
                @Override
                public int getDatabaseVersion() {
                    return sDatabaseVersion;
                }
            };

            SchemaBuilder.registerEntity(Invoice.class, sSchema);
            SchemaBuilder.registerEntity(Customer.class, sSchema);
            SchemaBuilder.registerEntity(Inventory.class, sSchema);
        }
        return sSchema;
    }

    static SQLSchema sSchema;
}
