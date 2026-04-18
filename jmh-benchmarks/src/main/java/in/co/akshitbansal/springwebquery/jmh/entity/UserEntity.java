package in.co.akshitbansal.springwebquery.jmh.entity;

import lombok.Data;

import java.util.List;

// @Entity
// @Table(name = "app_user")
@Data
public class UserEntity {

	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @Column(name = "user_id", nullable = false, unique = true)
	private String userId;

	// @OneToOne(cascade = CascadeType.ALL, optional = false)
	// @JoinColumn(name = "profile_id", nullable = false)
	private UserProfileEntity profile;

	// @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	// @JoinColumn(name = "user_id_fk", nullable = false)
	private List<AccountEntity> accounts;
}
