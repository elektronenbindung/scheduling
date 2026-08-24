package scheduling;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Integration test that runs {@link ConsoleRunner} (which exercises the
 * scheduling pipeline end-to-end) against
 * {@code src/test/IntegrationTest/Test_bad.ods} and verifies that the expected
 * output line is printed to the console.
 *
 * <p>
 * The test runs in a separate JVM process because
 * {@link scheduling.common.ThreadsController#} calls {@code System.exit(0)}
 * when not running in UI mode.
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
			List<String> stderr = new ArrayList<>();
			int exitCode = runOnce(projectRoot, inputFile, stdout, stderr);
			File outputFile = locateOutputFile(stdout, inputFile);

			try {
				assertEquals(0, exitCode,
						"Process exited with non-zero status.\nstdout: " + stdout + "\nstderr: " + stderr);

				int readIdx = stdout.indexOf(READ_SUCCESS_LINE);
				int solvableIdx = stdout.indexOf(SOLVABLE_SUCCESS_LINE);
				int firstCostsIdx = indexOfFirstCostsLine(stdout);
				int outputIdx = indexOfFirstLineWithPrefix(stdout, OUTPUT_PATH_PREFIX);

				assertTrue(readIdx >= 0, "Expected line '" + READ_SUCCESS_LINE + "' not found in stdout.\n"
						+ "Captured stdout: " + stdout + "\nCaptured stderr: " + stderr);
				assertTrue(solvableIdx >= 0, "Expected line '" + SOLVABLE_SUCCESS_LINE + "' not found in stdout.\n"
						+ "Captured stdout: " + stdout + "\nCaptured stderr: " + stderr);
				assertTrue(firstCostsIdx >= 0, "Expected at least one '" + COSTS_LINE_PREFIX + "...' line in stdout.\n"
						+ "Captured stdout: " + stdout + "\nCaptured stderr: " + stderr);
				assertTrue(outputIdx >= 0, "Expected line starting with '" + OUTPUT_PATH_PREFIX
						+ "' not found in stdout.\n" + "Captured stdout: " + stdout + "\nCaptured stderr: " + stderr);

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
						+ EXPECTED_COSTS + ".\n" + "Captured stdout: " + stdout + "\nCaptured stderr: " + stderr);
				assertTrue(outputFile != null && outputFile.exists(),
						"Output file was not created.\nstdout: " + stdout + "\nstderr: " + stderr);
				return;
			} catch (AssertionError e) {
				lastFailure = e;
			} finally {
				deleteQuietly(outputFile);
			}
		}
		throw lastFailure != null ? lastFailure : new AssertionError("No attempt was executed.");
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
			String prefix = "Writing output to: ";
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

	private int runOnce(File projectRoot, File inputFile, List<String> stdout, List<String> stderr) throws Exception {
		Process process = buildProcess(projectRoot, inputFile).start();
		Thread stdoutReader = captureStream(process.getInputStream(), stdout);
		Thread stderrReader = captureStream(process.getErrorStream(), stderr);

		boolean finished = process.waitFor(PER_RUN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		stdoutReader.join(5000);
		stderrReader.join(5000);

		if (!finished) {
			process.destroyForcibly();
			throw new AssertionError("Process did not finish within " + PER_RUN_TIMEOUT_SECONDS + " seconds.\n"
					+ "stdout: " + stdout + "\nstderr: " + stderr);
		}
		return process.exitValue();
	}

	private ProcessBuilder buildProcess(File projectRoot, File inputFile) {
		String classpath = System.getProperty("java.class.path");
		String javaHome = System.getProperty("java.home");
		String javaBin = Paths.get(javaHome, "bin", "java").toString();

		List<String> command = new ArrayList<>();
		command.add(javaBin);
		command.add("-cp");
		command.add(classpath);
		command.add(ConsoleRunner.class.getName());
		command.add(inputFile.getAbsolutePath());

		ProcessBuilder builder = new ProcessBuilder(command);
		builder.directory(projectRoot);
		builder.redirectErrorStream(false);
		return builder;
	}

	private Thread captureStream(InputStream stream, List<String> target) {
		Thread thread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					target.add(line);
				}
			} catch (IOException e) {
				target.add("[error reading stream]: " + e.getMessage());
			}
		}, "stream-reader");
		thread.setDaemon(true);
		thread.start();
		return thread;
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
}
