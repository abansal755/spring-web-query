package in.co.akshitbansal.springwebquery.util;

import org.testcontainers.containers.JdbcDatabaseContainer;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;

public class MySQLContainerFactory {

	/*
	 CORE PROBLEM:

	 We want a single test codebase that works with:
	 - Spring Boot 3 → Testcontainers 1.x
	 - Spring Boot 4 → Testcontainers 2.x

	 But Testcontainers changed the package of MySQLContainer:

	 1.x → org.testcontainers.containers.MySQLContainer
	 2.x → org.testcontainers.mysql.MySQLContainer

	 Java resolves imports at compile-time, not runtime.
	 So we cannot write code that imports both or switches dynamically.

	 RESULT:
	 A single source file cannot directly reference both versions.

	 SOLUTION:
	 Move the decision to runtime using reflection:
	 - Try loading 2.x class
	 - Fallback to 1.x class

	 We return JdbcDatabaseContainer because:
	 - It is stable across both versions
	 - It exposes the APIs we need (JDBC URL, username, password, init script)
	*/

	public static JdbcDatabaseContainer<?> createMySQLContainer(String imageName) {
		try {
			Constructor<? extends JdbcDatabaseContainer<?>> constructor = resolveConstructor();
			return constructor.newInstance(imageName);
		}
		catch (Exception ex) {
			throw new RuntimeException(MessageFormat.format(
					"Failed to create MySQLContainer: {0}", ex.getMessage()
			), ex);
		}
	}

	private static Constructor<? extends JdbcDatabaseContainer<?>> resolveConstructor() {
		// JdbcDatabaseContainer is stable across both the versions
		Class<? extends JdbcDatabaseContainer<?>> mysqlContainerClass;
		try {
			// Testcontainers 2.x
			// noinspection unchecked
			mysqlContainerClass = (Class<? extends JdbcDatabaseContainer<?>>)
					Class.forName("org.testcontainers.mysql.MySQLContainer");
		}
		catch (ClassNotFoundException ignored) {
			try {
				// Testcontainers 1.x
				// noinspection unchecked
				mysqlContainerClass = (Class<? extends JdbcDatabaseContainer<?>>)
						Class.forName("org.testcontainers.containers.MySQLContainer");
			}
			catch (ClassNotFoundException ex) {
				// Not found in either 1.x or 2.x
				throw new RuntimeException("Unable to find MySQLContainer class in Testcontainers 1.x or 2.x packages", ex);
			}
		}
		try {
			return mysqlContainerClass.getConstructor(String.class);
		}
		catch (NoSuchMethodException ex) {
			throw new RuntimeException("Unable to find MySQLContainer constructor with String parameter", ex);
		}
	}
}
