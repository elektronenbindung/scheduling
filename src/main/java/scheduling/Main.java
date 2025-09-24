package scheduling;

import java.io.File;
import java.io.Console;
import java.util.Objects;

import scheduling.common.ThreadsController;
import scheduling.ui.UiApplication;
import scheduling.common.Config;

public final class Main {

	private static final String UI_LAUNCH_HINT = "Hint: Provide exactly one single parameter on console as an input file - Using UI instead";
	private static final String CONSOLE_QUIT_HINT = "You can quit by typing '";

	private Main() {
	}

	public static void main(String[] args) {
		if (args.length == 1 && Objects.equals(args[0], Config.VERSION)) {
			System.out.println(AppVersion.getVersion());
			System.exit(0);
		}

		Console console = System.console();

		if (args.length != 1 || console == null) {
			System.out.println(UI_LAUNCH_HINT);
			UiApplication.launch(UiApplication.class, args);
		} else {
			File input = new File(args[0]);
			runConsoleMode(input, console);
		}
	}

	private static void runConsoleMode(File input, Console console) {
		System.out.println(CONSOLE_QUIT_HINT + Config.QUIT + "'");

		ThreadsController threadsController = new ThreadsController(input, null);
		Thread controllerThread = new Thread(threadsController);
		controllerThread.start();

		try {
			while (true) {
				String line = console.readLine();

				if (line == null || Objects.equals(line, Config.QUIT)) {
					threadsController.stop();
					break;
				}
			}
		} finally {
			System.exit(0);
		}
	}
}
