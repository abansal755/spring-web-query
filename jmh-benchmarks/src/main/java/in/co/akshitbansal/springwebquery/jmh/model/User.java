package in.co.akshitbansal.springwebquery.jmh.model;

import lombok.Data;

import java.util.List;

@Data
public class User {

	private String userId;
	private UserProfile profile;
	private List<Account> accounts;
}
