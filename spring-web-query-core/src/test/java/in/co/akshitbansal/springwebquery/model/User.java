package in.co.akshitbansal.springwebquery.model;

import lombok.Data;

import java.util.List;

@Data
public class User {

	private String email;
	private Name name;
	private Phone[] phones;
	private List<Address> addresses;
}
