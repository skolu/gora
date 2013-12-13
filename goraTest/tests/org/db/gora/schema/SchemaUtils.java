package org.db.gora.schema;

import org.db.gora.DataIntegrityException;
import org.db.gora.SQLiteSchema;
import org.db.gora.TableData;
import org.db.gora.TableLinkData;

import java.util.List;

/**
 * User: skolupaev
 * Date: 12/3/13
 */
public class SchemaUtils {

    public static SQLiteSchema getSchema() {
        if (sSchema == null) {
            sSchema = new SQLiteSchema() {
                @Override
                public int getDatabaseVersion() {
                    return 1;
                }
            };
            populateSchema();
        }
        return sSchema;
    }

    static void populateSchema() {
        try {
            SchemaBuilder.ClassInfo classInfo = SchemaBuilder.extractClassInfo(Invoice.class);
            TableData table = null;
            table = SchemaBuilder.createTableData(classInfo);

            sSchema.registerTableData(table);

            List<TableLinkData> linkData = SchemaBuilder.extractLinkData(classInfo, table);
            for (TableLinkData link: linkData) {
                sSchema.registerEntityLink(link);
            }

            createChildren(classInfo);

            classInfo = SchemaBuilder.extractClassInfo(Inventory.class);
            table = SchemaBuilder.createTableData(classInfo);
            sSchema.registerTableData(table);

            classInfo = SchemaBuilder.extractClassInfo(Customer.class);
            table = SchemaBuilder.createTableData(classInfo);
            sSchema.registerTableData(table);

        } catch (DataIntegrityException e) {
            e.printStackTrace();
        }
    }

    static void createChildren(SchemaBuilder.ClassInfo classInfo) throws DataIntegrityException {
        List<SchemaBuilder.ChildInfo> children = SchemaBuilder.extractChildInfo(classInfo);
        for (SchemaBuilder.ChildInfo child: children) {
            sSchema.registerTableData(child.childData);
            sSchema.registerChildTable(child.childLink);

            createChildren(child.childClassInfo);
        }
    }

    static SQLiteSchema sSchema;
}
