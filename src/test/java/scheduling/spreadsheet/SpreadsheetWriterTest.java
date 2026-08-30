package scheduling.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import scheduling.common.Config;
import scheduling.common.Solution;
import scheduling.common.ThreadsController;

/**
 * Integration test for {@link SpreadsheetWriter} focusing on the unique
 * output-file numbering logic. A real input {@code .ods} is loaded via
 * {@link SpreadsheetReader} so that {@code SpreadsheetWriter} operates against
 * a genuine {@link com.github.miachm.sods.SpreadSheet}, and {@code run()} is
 * invoked repeatedly to verify that existing output files cause a counter
 * suffix to be appended.
 */
class SpreadsheetWriterTest {

	private static final String TEST_INPUT_DIR = Paths.get("src", "test", "java", "scheduling").toString();
	private static final String INPUT_FILE_NAME = "Test.ods";

	@Test
	void appendsCounterSuffixWhenOutputFileAlreadyExists(@TempDir Path tempDir) throws Exception {
		File input = copyInputTo(tempDir);
		ThreadsController threadsController = new ThreadsController(input, null);
		threadsController.getSpreadsheetReader().run();

		for (int i = 0; i < 3; i++) {
			Solution solution = emptySolution(threadsController.getSpreadsheetReader());
			new SpreadsheetWriter(solution, threadsController).run();
		}

		File parent = input.getAbsoluteFile().getParentFile();
		File plain = new File(parent, "Test_output.ods");
		File numbered1 = new File(parent, "Test_output(1).ods");
		File numbered2 = new File(parent, "Test_output(2).ods");

		assertTrue(plain.exists(), "Expected 'Test_output.ods' to be created.\nfiles: " + listOdsFiles(parent));
		assertTrue(numbered1.exists(), "Expected 'Test_output(1).ods' to be created.\nfiles: " + listOdsFiles(parent));
		assertTrue(numbered2.exists(), "Expected 'Test_output(2).ods' to be created.\nfiles: " + listOdsFiles(parent));
		assertFalse(new File(parent, "Test_output(3).ods").exists(),
				"Did not expect a third numbered output file.\nfiles: " + listOdsFiles(parent));
	}

	private File copyInputTo(Path tempDir) throws IOException {
		Path projectRoot = findProjectRoot();
		Path source = projectRoot.resolve(Paths.get(TEST_INPUT_DIR, INPUT_FILE_NAME));
		Path target = tempDir.resolve(INPUT_FILE_NAME);
		Files.copy(source, target);
		return target.toFile();
	}

	private Solution emptySolution(SpreadsheetReader reader) {
		int lengthOfMonth = reader.getLengthOfMonth();
		int[] solution = new int[lengthOfMonth];
		Arrays.fill(solution, Config.MISSING_EMPLOYEE);
		int[] numberOfFreeDays = new int[Config.NUMBER_OF_EMPLOYEES];
		return new Solution(solution, numberOfFreeDays, reader, null);
	}

	private Path findProjectRoot() {
		Path current = Path.of(".").toAbsolutePath().normalize();
		Path root = current;
		while (root != null && !root.resolve("pom.xml").toFile().exists()) {
			root = root.getParent();
		}
		if (root == null) {
			throw new IllegalStateException("Could not locate project root (pom.xml) from " + current);
		}
		return root;
	}

	private List<String> listOdsFiles(File dir) {
		List<String> names = new ArrayList<>();
		File[] files = dir.listFiles((_, n) -> n.endsWith(".ods"));
		if (files != null) {
			for (File f : files) {
				names.add(f.getName());
			}
		}
		return names;
	}
}
