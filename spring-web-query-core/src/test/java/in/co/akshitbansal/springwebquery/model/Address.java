package in.co.akshitbansal.springwebquery.model;

import lombok.Data;

@Data
public class Address {

	// Intentionally not annotated with @MapsTo to test failure scenarios
	private String city;
}
