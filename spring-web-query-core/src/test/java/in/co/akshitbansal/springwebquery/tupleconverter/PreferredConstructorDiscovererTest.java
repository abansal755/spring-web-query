package in.co.akshitbansal.springwebquery.tupleconverter;

import in.co.akshitbansal.springwebquery.exception.QueryConfigurationException;
import in.co.akshitbansal.springwebquery.model.Address;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.hibernate.sql.results.internal.TupleElementImpl;
import org.hibernate.sql.results.internal.TupleImpl;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.PersistenceCreator;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class PreferredConstructorDiscovererTest {

	private final PreferredConstructorDiscoverer<Address> discoverer = PreferredConstructorDiscoverer.of(Address.class);

	@Test
	void testConstructionWithNullClass() {
		assertThrows(NullPointerException.class, () -> PreferredConstructorDiscoverer.of(null));
	}

	@Test
	void testConstructionWithNonNullClass() {
		assertDoesNotThrow(() -> PreferredConstructorDiscoverer.of(Address.class));
	}

	@Test
	void testWithNullTuple() {
		assertThrows(NullPointerException.class, () -> discoverer.discover(null));
	}

	@Test
	void testWithInvalidTuple() {
		TupleElement<Integer> tupleElement = new TupleElementImpl<>(int.class, "id");
		TupleMetadata metadata = new TupleMetadata(new TupleElement[]{ tupleElement }, new String[]{ "id" });
		Tuple tuple = new TupleImpl(metadata, new Object[]{ 1 });
		QueryConfigurationException ex = assertThrows(QueryConfigurationException.class, () -> discoverer.discover(tuple));
		assertTrue(ex.getMessage().contains("No suitable constructor"));
	}

	@Test
	void testWithValidTuple() {
		TupleElement<String> tupleElement = new TupleElementImpl<>(String.class, "city");
		TupleMetadata metadata = new TupleMetadata(new TupleElement[]{ tupleElement }, new String[]{ "city" });
		Tuple tuple = new TupleImpl(metadata, new Object[]{ "city" });
		Constructor<Address> constructor = discoverer.discover(tuple);
		assertTrue(constructor.isAnnotationPresent(PersistenceCreator.class));
	}
}
