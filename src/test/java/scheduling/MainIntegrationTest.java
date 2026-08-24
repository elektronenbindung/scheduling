package scheduling;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import scheduling.common.ThreadsController;

/**
 * Integration test that runs the scheduling pipeline end-to-end against
 * {@code src/test/java/scheduling/Test.ods} and verifies that the expected
 * output line is printed to the console.
 *
 * <p>
 * The pipeline runs in the test JVM so that JaCoCo can record coverage. A
 * {@link CountDownLatch} is used as the finish callback so the controller
 * terminates without calling {@code System.exit}. Once the expected solution
 * costs are observed, {@link ThreadsController#stop()} is called so that all
 * still-running TabuSearch threads exit promptly.
 */
class MainIntegrationTest {

	private static final String TEST_INPUT_RESOURCE_PATH = Paths.get("src", "test", "java", "scheduling", "Test.ods")
			.toString();

	private static final String COSTS_LINE_PREFIX = "Costs of solution: ";
	private static final double EXPECTED_COSTS = 323.0;

	private static final String READ_SUCCESS_LINE = "Input file has been read successfully, computing solutions...";
	private static final String SOLVABLE_SUCCESS_LINE = "Success: This schedule is solvable";
	private static final String OUTPUT_PATH_PREFIX = "Writing output to: ";

	private static final long PER_RUN_TIMEOUT_SECONDS = 120;
	private static final int MAX_ATTEMPTS = 3;

	@Test
	@Timeout(value = PER_RUN_TIMEOUT_SECONDS * MAX_ATTEMPTS, unit = TimeUnit.SECONDS)
	void runsSchedulingPipelineAndOutputsExpectedCosts() throws Exception {
		File projectRoot = findProjectRoot();
		File inputFile = projectRoot.toPath().resolve(TEST_INPUT_RESOURCE_PATH).toFile();
		assertTrue(inputFile.exists(), "Input file not found: " + inputFile.getAbsolutePath());

		AssertionError lastFailure = null;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			List<String> stdout = new ArrayList<>();
			File outputFile = null;
			try {
				outputFile = runOnceInVm(inputFile, stdout);
			} catch (TimeoutException e) {
				throw e;
			}

			try {
				int readIdx = stdout.indexOf(READ_SUCCESS_LINE);
				int solvableIdx = stdout.indexOf(SOLVABLE_SUCCESS_LINE);
				int firstCostsIdx = indexOfFirstCostsLine(stdout);
				int outputIdx = indexOfFirstLineWithPrefix(stdout, OUTPUT_PATH_PREFIX);

				assertTrue(readIdx >= 0, "Expected line '" + READ_SUCCESS_LINE + "' not found in stdout.\n"
						+ "Captured stdout: " + stdout);
				assertTrue(solvableIdx >= 0, "Expected line '" + SOLVABLE_SUCCESS_LINE + "' not found in stdout.\n"
						+ "Captured stdout: " + stdout);
				assertTrue(firstCostsIdx >= 0, "Expected at least one '" + COSTS_LINE_PREFIX + "...' line in stdout.\n"
						+ "Captured stdout: " + stdout);
				assertTrue(outputIdx >= 0, "Expected line starting with '" + OUTPUT_PATH_PREFIX
						+ "' not found in stdout.\n" + "Captured stdout: " + stdout);

				assertTrue(readIdx < solvableIdx, "Read-success line must appear before solvability line.\n"
						+ "readIdx=" + readIdx + " solvableIdx=" + solvableIdx + "\n" + "Captured stdout: " + stdout);
				assertTrue(solvableIdx < firstCostsIdx,
						"Solvability line must appear before first costs line.\n" + "solvableIdx=" + solvableIdx
								+ " firstCostsIdx=" + firstCostsIdx + "\n" + "Captured stdout: " + stdout);
				assertTrue(firstCostsIdx < outputIdx,
						"First costs line must appear before output-path line.\n" + "firstCostsIdx=" + firstCostsIdx
								+ " outputIdx=" + outputIdx + "\n" + "Captured stdout: " + stdout);

				double bestCosts = extractBestCosts(stdout);
				assertEquals(EXPECTED_COSTS, bestCosts, "Best solution costs " + bestCosts + " is not equal to "
						+ EXPECTED_COSTS + ".\n" + "Captured stdout: " + stdout);
				assertTrue(outputFile != null && outputFile.exists(),
						"Output file was not created.\nstdout: " + stdout);
				return;
			} catch (AssertionError e) {
				lastFailure = e;
			} finally {
				deleteQuietly(outputFile);
			}
		}
		throw lastFailure != null ? lastFailure : new AssertionError("No attempt was executed.");
	}

	private File runOnceInVm(File inputFile, List<String> stdoutSink) throws Exception {
		LineCapturingStream capturing = new LineCapturingStream(stdoutSink);
		PrintStream originalOut = System.out;
		PrintStream capturedOut = new PrintStream(capturing, true, StandardCharsets.UTF_8);
		System.setOut(capturedOut);

		CountDownLatch finishedLatch = new CountDownLatch(1);
		ThreadsController threadsController = new ThreadsController(inputFile, null, finishedLatch::countDown);

		Thread controllerThread = new Thread(threadsController, "ThreadsController");
		controllerThread.setDaemon(true);
		controllerThread.start();

		Thread stopper = new Thread(() -> {
			while (!Thread.currentThread().isInterrupted() && finishedLatch.getCount() > 0) {
				for (String line : stdoutSink) {
					if (line.startsWith(COSTS_LINE_PREFIX)) {
						try {
							double costs = Double.parseDouble(line.substring(COSTS_LINE_PREFIX.length()).trim());
							if (costs <= EXPECTED_COSTS) {
								threadsController.stop();
								return;
							}
						} catch (NumberFormatException ignored) {
							// skip unparseable line
						}
					}
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					return;
				}
			}
		}, "stopper");
		stopper.setDaemon(true);
		stopper.start();

		boolean finished = finishedLatch.await(PER_RUN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		System.setOut(originalOut);
		capturedOut.flush();
		stopper.interrupt();

		if (!finished) {
			threadsController.stop();
			throw new TimeoutException("Pipeline did not finish within " + PER_RUN_TIMEOUT_SECONDS + " seconds.\n"
					+ "Captured stdout: " + stdoutSink);
		}

		return locateOutputFile(stdoutSink, inputFile);
	}

	private double extractBestCosts(List<String> stdout) {
		double bestCosts = Double.POSITIVE_INFINITY;
		for (String line : stdout) {
			if (line.startsWith(COSTS_LINE_PREFIX)) {
				try {
					double costs = Double.parseDouble(line.substring(COSTS_LINE_PREFIX.length()).trim());
					if (costs < bestCosts) {
						bestCosts = costs;
					}
				} catch (NumberFormatException ignored) {
					// skip unparseable line
				}
			}
		}
		return bestCosts;
	}

	private int indexOfFirstCostsLine(List<String> stdout) {
		return indexOfFirstLineWithPrefix(stdout, COSTS_LINE_PREFIX);
	}

	private int indexOfFirstLineWithPrefix(List<String> stdout, String prefix) {
		for (int i = 0; i < stdout.size(); i++) {
			if (stdout.get(i).startsWith(prefix)) {
				return i;
			}
		}
		return -1;
	}

	@AfterEach
	void cleanupLeftoverOutputFiles() {
		File projectRoot = findProjectRoot();
		File inputFile = projectRoot.toPath().resolve(TEST_INPUT_RESOURCE_PATH).toFile();
		File parent = inputFile.getParentFile();
		if (parent == null || !parent.isDirectory()) {
			return;
		}
		String baseName = inputFile.getName().replace(".ods", "");
		File[] siblings = parent
				.listFiles((dir, name) -> name.startsWith(baseName + "_output") && name.endsWith(".ods"));
		if (siblings != null) {
			for (File file : siblings) {
				deleteQuietly(file);
			}
		}
	}

	private File locateOutputFile(List<String> stdout, File inputFile) {
		for (String line : stdout) {
			String prefix = OUTPUT_PATH_PREFIX;
			if (line.startsWith(prefix)) {
				return new File(line.substring(prefix.length()).trim());
			}
		}
		File fallback = new File(inputFile.getParentFile(), inputFile.getName().replace(".ods", "_output.ods"));
		return fallback.exists() ? fallback : null;
	}

	private void deleteQuietly(File file) {
		if (file == null || !file.exists()) {
			return;
		}
		try {
			file.delete();
		} catch (Exception ignored) {
			// best effort cleanup
		}
	}

	private File findProjectRoot() {
		Path current = Paths.get(".").toAbsolutePath().normalize();
		Path root = current;
		while (root != null && !root.resolve("pom.xml").toFile().exists()) {
			root = root.getParent();
		}
		if (root == null) {
			throw new IllegalStateException("Could not locate project root (pom.xml) from " + current);
		}
		return root.toFile();
	}

	private static final class TimeoutException extends Exception {
		TimeoutException(String message) {
			super(message);
		}
	}

	private static final class LineCapturingStream extends OutputStream {
		private final List<String> sink;
		private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		LineCapturingStream(List<String> sink) {
			this.sink = sink;
		}

		@Override
		public synchronized void write(int b) {
			if (b == '\n') {
				flushLine();
			} else if (b != '\r') {
				buffer.write(b);
			}
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) {
			for (int i = off; i < off + len; i++) {
				write(b[i] & 0xFF);
			}
		}

		private void flushLine() {
			String line = buffer.toString(StandardCharsets.UTF_8);
			buffer.reset();
			sink.add(line);
		}
	}
}
