package scheduling.spreadsheet;

import java.io.File;
import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;

import scheduling.common.Config;
import scheduling.common.ThreadsController;
import scheduling.common.Solution;

public class SpreadsheetWriter {
	private final Solution solution;
	private final ThreadsController threadsController;

	public SpreadsheetWriter(Solution solution, ThreadsController threadsController) {
		this.solution = solution;
		this.threadsController = threadsController;
	}

	public void run() throws Exception {
		File outputFile = generateUniqueOutputFile();

		String[][] output = getOutput();
		saveOutput(outputFile, output);
	}

	private File generateUniqueOutputFile() {
		File inputFile = threadsController.getSpreadsheetReader().getInputFile();
		String parent = inputFile.getAbsoluteFile().getParent();
		String originalName = inputFile.getName();

		int dotIndex = originalName.lastIndexOf('.');
		String baseName = (dotIndex == -1) ? originalName : originalName.substring(0, dotIndex);
		String extension = ".ods";

		String candidateName = baseName + "_output" + extension;
		File candidateFile = new File(parent, candidateName);

		int counter = 1;
		while (candidateFile.exists()) {
			candidateName = baseName + "_output(" + counter + ")" + extension;
			candidateFile = new File(parent, candidateName);
			counter++;
		}
		return candidateFile;
	}

	private String[][] getOutput() {
		String[][] output = new String[Config.NUMBER_OF_EMPLOYEES][31];

		for (int day = 0; day < threadsController.getSpreadsheetReader().getLengthOfMonth(); day++) {
			int employee = solution.getEmployeeForDay(day);
			if (employee != Config.MISSING_EMPLOYEE) {
				output[employee][day] = Config.WORKING;
			}
		}
		return output;
	}

	private void saveOutput(File outputFile, String[][] output) throws Exception {
		threadsController.println("Writing output to: " + outputFile.getAbsolutePath());

		Sheet sheet = threadsController.getSpreadsheetReader().getSheet();
		Range range = sheet.getRange("B6:AF" + Config.LAST_ROW_OF_SCHEDULE);
		range.setValues(output);
		SpreadSheet spreadSheet = threadsController.getSpreadsheetReader().getSpreadSheet();
		spreadSheet.save(outputFile);
	}
}
