package in.co.akshitbansal.springwebquery.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.PersistenceCreator;

@Data
@NoArgsConstructor
public class Address {

	// Intentionally not annotated with @MapsTo to test failure scenarios
	private String city;

	@PersistenceCreator
	private Address(String city) {
		this.city = city;
	}
}
