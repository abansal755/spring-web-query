package in.co.akshitbansal.springwebquery.jmh.entity;

import lombok.Data;

// @Entity
// @Table(name = "issuer")
@Data
public class IssuerEntity {

	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @Column(name = "legal_name", nullable = false)
	private String legalName;

	// @ManyToOne(cascade = CascadeType.ALL, optional = false)
	// @JoinColumn(name = "compliance_id", nullable = false)
	private ComplianceEntity compliance;
}
