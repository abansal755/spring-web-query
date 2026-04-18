package in.co.akshitbansal.springwebquery.jmh.entity;

import lombok.Data;

import java.util.List;

// @Entity
// @Table(name = "account")
@Data
public class AccountEntity {

	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @Column(name = "account_number", nullable = false, unique = true)
	private String accountNumber;

	// @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	// @JoinColumn(name = "account_id_fk", nullable = false)
	private List<PortfolioEntity> portfolios;
}
