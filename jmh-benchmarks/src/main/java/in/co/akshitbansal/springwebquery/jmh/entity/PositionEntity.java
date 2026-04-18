package in.co.akshitbansal.springwebquery.jmh.entity;

import lombok.Data;

import java.util.List;

// @Entity
// @Table(name = "position")
@Data
public class PositionEntity {

	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @Column(name = "symbol", nullable = false)
	private String symbol;

	// @ManyToOne(cascade = CascadeType.ALL, optional = false)
	// @JoinColumn(name = "security_id", nullable = false)
	private SecurityEntity security;

	// @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	// @JoinColumn(name = "position_id_fk", nullable = false)
	private List<LotEntity> lots;
}
