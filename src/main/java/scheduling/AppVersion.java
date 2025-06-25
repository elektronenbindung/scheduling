package scheduling;

public class AppVersion {
	public static String getVersion() {
		Package mainPackage = Main.class.getPackage();
		String version = mainPackage.getImplementationVersion();
		return version != null ? version : "dev";
	}
}
