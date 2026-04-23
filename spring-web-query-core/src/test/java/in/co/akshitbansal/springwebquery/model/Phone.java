package in.co.akshitbansal.springwebquery.model;

import in.co.akshitbansal.springwebquery.annotation.MapsTo;
import lombok.Data;

@Data
public class Phone {

	@MapsTo("phoneNumber")
	private String number;
}
