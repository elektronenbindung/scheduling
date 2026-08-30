package scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import scheduling.common.Config;

/**
 * Tests for the dispatch logic of {@link Main}.
 *
 * <p>
 * {@link Main#main} cannot be driven directly from the test JVM because the
 * version branch calls {@code System.exit}, the UI branch launches JavaFX and
 * the console branch blocks on {@link System#console()}. The branching decision
 * itself is therefore extracted into the pure, side-effect-free
 * {@link Main#determineMode} method, which is exercised here in-VM so that
 * JaCoCo records coverage of {@code Main.java}.
 */
class MainTest {

	@Test
	void versionArgumentSelectsVersionMode() {
		assertEquals(Main.Mode.VERSION, Main.determineMode(new String[]{Config.VERSION}, true));
		assertEquals(Main.Mode.VERSION, Main.determineMode(new String[]{Config.VERSION}, false));
	}

	@Test
	void noArgumentsSelectsUiMode() {
		assertEquals(Main.Mode.UI, Main.determineMode(new String[]{}, true));
		assertEquals(Main.Mode.UI, Main.determineMode(new String[]{}, false));
	}

	@Test
	void multipleArgumentsSelectUiMode() {
		assertEquals(Main.Mode.UI, Main.determineMode(new String[]{"a", "b"}, true));
		assertEquals(Main.Mode.UI, Main.determineMode(new String[]{"a", "b"}, false));
	}

	@Test
	void singleArgumentWithoutConsoleSelectsUiMode() {
		assertEquals(Main.Mode.UI, Main.determineMode(new String[]{"input.ods"}, false));
	}

	@Test
	void singleArgumentWithConsoleSelectsConsoleMode() {
		assertEquals(Main.Mode.CONSOLE, Main.determineMode(new String[]{"input.ods"}, true));
	}

	@Test
	void nonVersionSingleArgumentIsNotTreatedAsVersion() {
		assertEquals(Main.Mode.CONSOLE, Main.determineMode(new String[]{"--version "}, true));
		assertEquals(Main.Mode.UI, Main.determineMode(new String[]{"--version", "extra"}, true));
	}
}
