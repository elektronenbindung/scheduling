package scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class AppVersionTest {

	@Test
	void returnsDevWhenImplementationVersionIsNotSet() {
		String version = AppVersion.getVersion();

		assertNotNull(version);
		assertEquals("dev", version);
	}
}
