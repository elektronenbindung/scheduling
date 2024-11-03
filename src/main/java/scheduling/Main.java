package scheduling;

import java.io.File;

import scheduling.common.ThreadsController;
import scheduling.common.Config;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println(
                    "Hint: Provide exactly one single parameter on console as an input file - Using UI instead");
            new UI(getVersion()).setVisible(true);
        } else {
            if (args[0].equals(Config.VERSION)) {
                System.out.println("version: " + getVersion());
                System.exit(0);
            }
            File input = new File(args[0]);
            ThreadsController threadsController = new ThreadsController(input, null);
            new Thread(threadsController).start();
            System.out.println("You can quit by typing '" + Config.QUIT + "'");

            while (true) {
                String line = System.console().readLine();

                if (line.equals(Config.QUIT)) {
                    threadsController.stop();
                }
            }
        }

    }

    private static String getVersion() {
        Package mainPackage = Main.class.getPackage();
        String version = mainPackage.getImplementationVersion();
        return version;
    }

}
