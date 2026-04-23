package in.co.akshitbansal.springwebquery.model;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import lombok.Data;

@Data
public class Name {

	@MapsTo(value = "firstName", absolute = true)
	private String firstName;

	@MapsTo(value = "lastName", absolute = true)
	private String lastName;
}
