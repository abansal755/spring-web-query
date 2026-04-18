package in.co.akshitbansal.springwebquery.jmh.model;

import lombok.Data;

import java.util.List;

@Data
public class Account {

	private String accountNumber;
	private List<Portfolio> portfolios;
}
