package org.db.gora.schema;

import org.db.gora.EntityEvent;

import java.util.Date;

public class Entity extends Row  implements EntityEvent {
	@SqlColumn(name="cloud_key", index=true)
	public String cloudKey;

	@SqlColumn(name="cloud_token")
	public String cloudToken;

	@SqlColumn(name="created")
    public Date created;
	
	@SqlColumn(name="modified", index = true)
    public Date modified;

    @SqlColumn(name="type")
    public EntityType type;
	
	@SqlColumn(name="name", nullable=false, index=true)
	public String name;

    @Override
    public void onRead() {
    }

    @Override
    public boolean onWrite() {
        if (name == null) {
            return false;
        }
        if (getId() == 0) {
            created = new Date();
        }
        modified = new Date();
        if (type == null) {
            type = EntityType.Regular;
        }
        return true;
    }

}
