package org.db.gora.schema;

import org.db.gora.SQLiteEvent;

import java.util.Date;

public class Entity extends Row  implements SQLiteEvent {
	@SqlColumn(name="cloud_key", index=true)
	public String cloudKey;

	@SqlColumn(name="cloud_token")
	public String cloudToken;

	@SqlColumn(name="created")
    public Date created;
	
	@SqlColumn(name="modified", index = true)
    public Date modified;
	
	@SqlColumn(name="name", nullable=false, index=true)
	public String name;

    @Override
    public void onRead() {
    }

    @Override
    public boolean onWrite() {
        if (getId() == 0) {
            created = new Date();
        }
        modified = new Date();
        return true;
    }

}
