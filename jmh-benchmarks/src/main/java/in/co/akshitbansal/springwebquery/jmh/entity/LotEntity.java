package in.co.akshitbansal.springwebquery.jmh.entity;

import lombok.Data;

// @Entity
// @Table(name = "lot")
@Data
public class LotEntity {

	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @Column(name = "serial_number", nullable = false, unique = true)
	private String serialNumber;
}
