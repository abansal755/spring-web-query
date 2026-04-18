package in.co.akshitbansal.springwebquery.jmh.entity;

import lombok.Data;

import java.util.List;

// @Entity
// @Table(name = "user_profile")
@Data
public class UserProfileEntity {

	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @Column(name = "external_id", nullable = false, unique = true)
	private String externalId;

	// @OneToOne(cascade = CascadeType.ALL)
	// @JoinColumn(name = "primary_address_id")
	private AddressEntity primaryAddress;

	// @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	// @JoinTable(
	//     name = "user_profile_alternate_address",
	//     joinColumns = @JoinColumn(name = "profile_id"),
	//     inverseJoinColumns = @JoinColumn(name = "address_id")
	// )
	private List<AddressEntity> alternateAddresses;
}
