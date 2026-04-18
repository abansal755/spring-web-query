package in.co.akshitbansal.springwebquery.jmh.model;

import lombok.Data;

import java.util.List;

@Data
public class UserProfile {

	private String externalId;
	private Address primaryAddress;
	private List<Address> alternateAddresses;
}
