package scheduling;

import java.io.File;

import scheduling.common.ThreadsController;
import scheduling.ui.UiApplication;
import scheduling.common.Config;

public final class Main {

	private Main() {
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println(
					"Hint: Provide exactly one single parameter on console as an input file - Using UI instead");
			UiApplication.launch(UiApplication.class, args);
		} else {
			if (args[0].equals(Config.VERSION)) {
				System.out.println(AppVersion.getVersion());
				System.exit(0);
			}
			System.out.println("You can quit by typing '" + Config.QUIT + "'");
			File input = new File(args[0]);
			ThreadsController threadsController = new ThreadsController(input, null);
			new Thread(threadsController).start();

			// noinspection InfiniteLoopStatement
			while (true) {
				String line = System.console().readLine();

				if (line.equals(Config.QUIT)) {
					threadsController.stop();
				}
			}
		}
	}

}
