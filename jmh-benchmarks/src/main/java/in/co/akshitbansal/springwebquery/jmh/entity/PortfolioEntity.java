package in.co.akshitbansal.springwebquery.jmh.entity;

import lombok.Data;

import java.util.List;

// @Entity
// @Table(name = "portfolio")
@Data
public class PortfolioEntity {

	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @Column(name = "portfolio_code", nullable = false)
	private String portfolioCode;

	// @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	// @JoinColumn(name = "portfolio_id_fk", nullable = false)
	private List<PositionEntity> positions;
}
