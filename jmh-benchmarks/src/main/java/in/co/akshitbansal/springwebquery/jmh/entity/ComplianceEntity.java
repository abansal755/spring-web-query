package in.co.akshitbansal.springwebquery.jmh.entity;

import lombok.Data;

// @Entity
// @Table(name = "compliance")
@Data
public class ComplianceEntity {

	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @Column(name = "market_region", nullable = false)
	private String marketRegion;
}
