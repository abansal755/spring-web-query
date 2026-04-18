package in.co.akshitbansal.springwebquery.jmh.model;

import lombok.Data;

import java.util.List;

@Data
public class Position {

	private String symbol;
	private Security security;
	private List<Lot> lots;
}
