package in.co.akshitbansal.springwebquery.jmh.entity;

import lombok.Data;

// @Entity
// @Table(name = "address")
@Data
public class AddressEntity {

	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @Column(name = "city", nullable = false)
	private String city;

	// @Column(name = "postal_code", nullable = false)
	private String postalCode;
}
