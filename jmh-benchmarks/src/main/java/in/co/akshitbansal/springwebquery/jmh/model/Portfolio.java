package in.co.akshitbansal.springwebquery.jmh.model;

import lombok.Data;

import java.util.List;

@Data
public class Portfolio {

	private String portfolioCode;
	private List<Position> positions;
}
