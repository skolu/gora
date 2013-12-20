package org.db.gora.schema;

import java.util.ArrayList;
import java.util.List;

@SqlTable(name="Customer")
public class Customer extends Entity {
	@SqlColumn(name="first_name")
	public String firstName;

	@SqlColumn(name="last_name")
	public String lastName;

    @SqlChild(classes={Address.class}, getter="getAddresses")
    List<Address> addresses;
    public List<Address> getAddresses() {
        if (addresses == null) {
            addresses = new ArrayList<Address>();
        }
        return addresses;
    }

}
