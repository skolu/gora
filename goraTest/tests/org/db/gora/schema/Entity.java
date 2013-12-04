package org.db.gora.schema;

import java.util.Date;

public class Entity extends Row {
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
}
