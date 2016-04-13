package org.db.gora.schema;

import java.util.ArrayList;
import java.util.List;

@SqlTable(name="Customer")
public class Customer extends Entity {
	@SqlColumn(name="first_name")
	public String firstName;

    @SqlColumn(name="last_name", getter = "getLastName", setter = "setLastName")
	private String lastName;

    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }


    @SqlChild(classes={Address.class}, getter="getAddresses")
    List<Address> addresses;
    public List<Address> getAddresses() {
        if (addresses == null) {
            addresses = new ArrayList<>();
        }
        return addresses;
    }

}
