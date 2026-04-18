package in.co.akshitbansal.springwebquery.jmh.entity;

import lombok.Data;

// @Entity
// @Table(name = "security")
@Data
public class SecurityEntity {

	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @Column(name = "isin", nullable = false, unique = true)
	private String isin;

	// @ManyToOne(cascade = CascadeType.ALL, optional = false)
	// @JoinColumn(name = "issuer_id", nullable = false)
	private IssuerEntity issuer;
}
