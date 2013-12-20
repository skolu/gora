package org.db.gora.schema;

@SqlTable(name="Address")
public class Address extends Row {
    @SqlColumn(name="customer_id", fk=true)
   	public long customerId;

    @SqlColumn(name="address_type", getter="getAddressType", setter="setAddressType")
    AddressType addressType;
    public AddressType getAddressType() {
        return addressType;
    }
    public void setAddressType(AddressType addressType) {
        this.addressType = addressType;
    }

    @SqlColumn(name="address1")
    public String address1;

    @SqlColumn(name="address2")
    public String address2;

    @SqlColumn(name="city")
    public String city;

    @SqlColumn(name="state")
    public String state;

    @SqlColumn(name="zip")
    public String zipCode;


    public static enum AddressType {
        Home,
        Work,
        Other,
    }

}
