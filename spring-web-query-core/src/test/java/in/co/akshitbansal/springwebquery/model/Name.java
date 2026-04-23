package in.co.akshitbansal.springwebquery.model;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import in.co.akshitbansal.springwebquery.annotation.Sortable;
import lombok.Data;

@Data
public class Name {

	@MapsTo(value = "firstName", absolute = true)
	@Sortable
	private String firstName;

	@MapsTo(value = "lastName", absolute = true)
	// Explicitly not annotated with @Sortable
	private String lastName;
}
