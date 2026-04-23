package in.co.akshitbansal.springwebquery.entity;

import lombok.Data;

import java.util.List;

@Data
public class UserEntity {

	private Long id;
	private String email;
	private String firstName;
	private String lastName;
	private List<PhoneEntity> phones;
	private List<AddressEntity> addresses;
}
