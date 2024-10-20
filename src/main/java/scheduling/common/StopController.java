package scheduling.common;

public class StopController implements Runnable {
    private ThreadsController threadsController;

    public StopController(ThreadsController threadsController) {
        this.threadsController = threadsController;
    }

    public void run() {
        threadsController.stop();
    }

}
