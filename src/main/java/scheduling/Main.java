package scheduling;

import java.io.File;

import scheduling.common.Controller;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println(
                    "Hint: Provide exactly one single paramter on console as an input file - Using UI instead");
            new UI().setVisible(true);
        } else {
            File input = new File(args[0]);
            Controller controller = new Controller(input, null);
            controller.run();
            System.exit(0);
        }

    }

}
