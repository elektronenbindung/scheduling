package scheduling;

import java.util.Objects;

public final class AppVersion {

	private AppVersion() {
	}

	public static String getVersion() {
		Package mainPackage = Main.class.getPackage();

		String version = null;
		if (mainPackage != null) {
			version = mainPackage.getImplementationVersion();
		}

		return Objects.requireNonNullElse(version, "dev");
	}
}
