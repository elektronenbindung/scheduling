package scheduling;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import scheduling.common.Config;
import scheduling.common.Solution;
import scheduling.common.ThreadsController;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that runs the scheduling pipeline end-to-end against a set
 * of input files and verifies that the expected output is produced.
 *
 * <p>
 * The pipeline runs in the test JVM so that JaCoCo can record coverage. A
 * {@link CountDownLatch} is used as the finish callback so the controller
 * terminates without calling {@code System.exit}. Each test case is driven by a
 * {@link TestCase} record holding the input file, the expected solvability
 * line, the expected solution costs and the expected schedule as a
 * two-dimensional array {@code [employee][day]}.
 */
class MainIntegrationTest {

	private static final String COSTS_LINE_PREFIX = "Costs of solution: ";
	private static final String READ_SUCCESS_LINE = "Input file has been read successfully, computing solutions...";
	private static final String SOLVABLE_SUCCESS_LINE = "Success: This schedule is solvable";
	private static final String NOT_SOLVABLE_WARNING_LINE = "Warning: This schedule is not solvable";
	private static final String OUTPUT_PATH_PREFIX = "Writing output to: ";

	private static final long PER_RUN_TIMEOUT_SECONDS = 600;
	private static final int MAX_ATTEMPTS = 1;

	private static final long RANDOM_SEED = 42L;

	private static final int NUMBER_OF_PARALLEL_THREADS_OVERRIDE = 5;

	private static final String TEST_INPUT_DIR = Paths.get("src", "test", "java", "scheduling").toString();

	private static final String[][] EXPECTED_SCHEDULE_SOLVABLE = {
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "x",
					"x", "x", null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, "x", null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, "x", "x", "x", null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, "x", "x", "x", null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, "x", null, null, null, null, "x", null, null, null, null},
			{"x", "x", "x", "x", null, null, null, null, null, null, null, null, null, "x", "x", null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "x", "x", null,
					null, null, null, null, null, null, "x", "x", null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, "x", "x", null, null, null, null, null, null, null},
			{null, null, null, null, "x", null, null, null, "x", "x", null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, "x", "x", "x", null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null}};

	private static final String[][] EXPECTED_SCHEDULE_NOT_SOLVABLE = {
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "x",
					"x", "x", null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, "x", null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, "x", "x", "x", null, null, null, null, null,
					null, null, null, null, null, null, null, "x", "x", "x", null, null, null},
			{null, null, null, null, "x", null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, "x", null, null, null, null, null, null, null, null, null, null},
			{null, "x", "x", null, null, null, null, null, null, null, null, null, null, "x", null, null, null, null,
					null, null, null, null, null, "x", "x", null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "x", "x", null,
					null, null, null, null, null, null, null, null, null, null, "x", "x", null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, "x", "x", null, null, null, null, null, null, null, null},
			{null, null, null, "x", null, null, null, null, "x", "x", null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, "x", "x", "x", null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null}};

	private static final String[][] EXPECTED_SCHEDULE_STAGNATION = {
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, "x", "x", "x", null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, "x", "x", null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, "x", "x", "x", null, null, null, null, null, null, null, "x", null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, "x"},
			{"x", "x", null, null, null, "x", "x", null, null, null, null, null, null, null, null, "x", "x", null, null,
					null, null, null, null, null, "x", "x", null, null, null, null, null},
			{null, null, null, null, "x", null, null, "x", null, null, null, "x", null, null, null, null, null, null,
					"x", null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "x",
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, "x", "x", "x", null, null, null,
					null, null, null, null, null, null, null, null, "x", "x", "x", null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, "x", "x", null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null}};

	private static final String[][] EXPECTED_SCHEDULE_OPTIMAL_SOLUTION = {
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, "x", null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, "x", null, null, null, null, null},
			{"x", "x", "x", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, "x", "x"},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, "x", "x", "x", "x", "x", "x", null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, "x", "x", null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, "x", "x", "x", "x", "x", "x", "x", "x", null, null,
					null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "x",
					"x", null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, "x", null, null, null, "x", "x", null, null, null, "x", null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, "x", "x", null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null}};

	private static final String[][] EXPECTED_SCHEDULE_FIXED = {
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, "x", "x", "x", null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, "x", "x", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, "x", "x", "x", null, null, null, null, null, null, null, null, "x"},
			{null, null, null, null, "x", null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{"x", null, null, "x", null, "x", null, "x", null, null, null, null, null, null, null, "x", null, null, "x",
					null, null, null, null, "x", null, "x", null, null, null, null, null},
			{null, null, null, null, null, null, "x", null, null, null, null, "x", null, null, null, null, "x", "x",
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, "x", null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, "x", "x", "x", null, null, null,
					null, null, null, null, null, null, null, null, null, "x", "x", "x", null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, "x", null, "x", null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null},
			{null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null}};

	private record TestCase(String inputFileName, String expectedSolvabilityLine, double expectedCosts,
			String[][] expectedSchedule, boolean expectSolution, String expectedErrorLine,
			boolean expectedEarlyTermination) {
	}

	static Stream<Arguments> testCases() {
		return Stream.of(
				Arguments.of(new TestCase("Test.ods", SOLVABLE_SUCCESS_LINE, 323.0, EXPECTED_SCHEDULE_SOLVABLE, true,
						null, false)),
				Arguments.of(new TestCase("Test_not_solvable.ods", NOT_SOLVABLE_WARNING_LINE, 320.0,
						EXPECTED_SCHEDULE_NOT_SOLVABLE, true, null, false)),
				Arguments.of(new TestCase("Test_stagnation.ods", SOLVABLE_SUCCESS_LINE, 16.75,
						EXPECTED_SCHEDULE_STAGNATION, true, null, false)),
				Arguments.of(new TestCase("Test_optimal_solution.ods", SOLVABLE_SUCCESS_LINE, 0.0,
						EXPECTED_SCHEDULE_OPTIMAL_SOLUTION, true, null, true)),
				Arguments.of(new TestCase("Test_fixed_schedule.ods", SOLVABLE_SUCCESS_LINE, 36.625,
						EXPECTED_SCHEDULE_FIXED, true, null, false)),
				Arguments.of(new TestCase("Test_not_available.ods", null, -1, null, false,
						"Error: Error on day 11: An employee is scheduled to work but is marked as unavailable.",
						false)),
				Arguments.of(new TestCase("Test_multiple_employees_at_day.ods", null, -1, null, false,
						"Error: Error on day 11: Multiple employees are fixed for this day, but only one is allowed.",
						false)),
				Arguments.of(new TestCase("Test_not_existing.ods", null, -1, null, false,
						"Error: The provided input file does not exist or is not a file", false)));
	}

	@ParameterizedTest
	@MethodSource("testCases")
	@Timeout(value = PER_RUN_TIMEOUT_SECONDS * MAX_ATTEMPTS, unit = TimeUnit.SECONDS)
	void runsSchedulingPipelineAndOutputsExpectedCosts(TestCase testCase) throws Exception {
		System.setProperty("scheduling.numberOfParallelThreads", String.valueOf(NUMBER_OF_PARALLEL_THREADS_OVERRIDE));
		File projectRoot = findProjectRoot();
		File inputFile = projectRoot.toPath().resolve(Paths.get(TEST_INPUT_DIR, testCase.inputFileName())).toFile();

		AssertionError lastFailure = null;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			List<String> stdout = new ArrayList<>();
			ScheduleResult result;
			result = runOnceInVm(inputFile, stdout);

			try {
				if (!testCase.expectSolution()) {
					assertTrue(stdout.contains(testCase.expectedErrorLine()), "Expected error line '"
							+ testCase.expectedErrorLine() + "' not found in stdout.\n" + "Captured stdout: " + stdout);
					assertNull(result.schedule(), "Expected no schedule for error case.\nstdout: " + stdout);
					assertNull(result.outputFile(), "Output file was created.\nstdout: " + stdout);
					return;
				}

				int readIdx = stdout.indexOf(READ_SUCCESS_LINE);
				int solvableIdx = stdout.indexOf(testCase.expectedSolvabilityLine());
				int firstCostsIdx = indexOfFirstCostsLine(stdout);
				int outputIdx = indexOfFirstLineWithPrefix(stdout, OUTPUT_PATH_PREFIX);

				assertTrue(readIdx >= 0, "Expected line '" + READ_SUCCESS_LINE + "' not found in stdout.\n"
						+ "Captured stdout: " + stdout);
				assertTrue(solvableIdx >= 0, "Expected line '" + testCase.expectedSolvabilityLine()
						+ "' not found in stdout.\n" + "Captured stdout: " + stdout);
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
				assertEquals(testCase.expectedCosts(), bestCosts, "Best solution costs " + bestCosts
						+ " is not equal to " + testCase.expectedCosts() + ".\n" + "Captured stdout: " + stdout);
				assertTrue(result.outputFile() != null && result.outputFile().exists(),
						"Output file was not created.\nstdout: " + stdout);
				assertEquals(testCase.expectedEarlyTermination(), result.earlyTerminated(),
						"Early termination flag mismatch.\nstdout: " + stdout);

				String[][] actualSchedule = result.schedule();
				String[][] expectedSchedule = testCase.expectedSchedule();
				for (int employee = 0; employee < expectedSchedule.length; employee++) {
					for (int day = 0; day < expectedSchedule[employee].length; day++) {
						assertEquals(expectedSchedule[employee][day], actualSchedule[employee][day],
								"Mismatch at employee=" + employee + " day=" + day + ".\n" + "Full schedule:\n"
										+ formatSchedule(actualSchedule));
					}
				}
				return;
			} catch (AssertionError e) {
				lastFailure = e;
			}
		}
		throw lastFailure != null ? lastFailure : new AssertionError("No attempt was executed.");
	}

	private ScheduleResult runOnceInVm(File inputFile, List<String> stdoutSink) throws Exception {
		LineCapturingStream capturing = new LineCapturingStream(stdoutSink);
		PrintStream originalOut = System.out;
		PrintStream capturedOut = new PrintStream(capturing, true, StandardCharsets.UTF_8);
		System.setOut(capturedOut);

		CountDownLatch finishedLatch = new CountDownLatch(1);
		ThreadsController threadsController = new ThreadsController(inputFile, null, finishedLatch::countDown,
				RANDOM_SEED);

		Thread controllerThread = new Thread(threadsController, "ThreadsController");
		controllerThread.setDaemon(true);
		controllerThread.start();

		boolean finished = finishedLatch.await(PER_RUN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		System.setOut(originalOut);
		capturedOut.flush();

		if (!finished) {
			threadsController.stop();
			throw new TimeoutException("Pipeline did not finish within " + PER_RUN_TIMEOUT_SECONDS + " seconds.\n"
					+ "Captured stdout: " + stdoutSink);
		}

		File outputFile = locateOutputFile(stdoutSink, inputFile);
		String[][] schedule = buildScheduleFromSolution(threadsController);
		boolean earlyTerminated = threadsController.isStopped();
		return new ScheduleResult(outputFile, schedule, earlyTerminated);
	}

	private String[][] buildScheduleFromSolution(ThreadsController threadsController) {
		Solution solution = threadsController.getBestSolution();
		if (solution == null) {
			return null;
		}
		int lengthOfMonth = threadsController.getSpreadsheetReader().getLengthOfMonth();
		String[][] schedule = new String[Config.NUMBER_OF_EMPLOYEES][31];
		for (int day = 0; day < lengthOfMonth; day++) {
			int employee = solution.getEmployeeForDay(day);
			if (employee != Config.MISSING_EMPLOYEE) {
				schedule[employee][day] = Config.WORKING;
			}
		}
		return schedule;
	}

	private static String formatSchedule(String[][] schedule) {
		StringBuilder sb = new StringBuilder();
		for (int employee = 0; employee < schedule.length; employee++) {
			sb.append("Emp ").append(employee).append(": ");
			for (int day = 0; day < schedule[employee].length; day++) {
				sb.append(schedule[employee][day] == null ? "." : schedule[employee][day]).append(" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	private record ScheduleResult(File outputFile, String[][] schedule, boolean earlyTerminated) {
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
	void preserveOutputFiles() {
		File projectRoot = findProjectRoot();
		File sourceParent = projectRoot.toPath().resolve(TEST_INPUT_DIR).toFile();
		if (!sourceParent.isDirectory()) {
			return;
		}
		File[] siblings = sourceParent.listFiles((dir, name) -> name.endsWith("_output.ods"));
		if (siblings == null || siblings.length == 0) {
			return;
		}
		File targetDir = projectRoot.toPath().resolve(Paths.get("target", "test-output-ods")).toFile();
		if (!targetDir.exists() && !targetDir.mkdirs()) {
			return;
		}
		for (File file : siblings) {
			File dest = new File(targetDir, file.getName());
			try {
				Files.move(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.ATOMIC_MOVE);
			} catch (Exception atomicMoveFailed) {
				try {
					Files.move(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception ignored) {
					deleteQuietly(file);
				}
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
