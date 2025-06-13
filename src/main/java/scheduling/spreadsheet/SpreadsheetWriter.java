package scheduling.spreadsheet;

import java.io.File;
import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;

import scheduling.common.Config;
import scheduling.common.ThreadsController;
import scheduling.common.Solution;

public class SpreadsheetWriter {
  private Solution solution;
  private ThreadsController threadsController;

  public SpreadsheetWriter(Solution solution, ThreadsController threadsController) {
    this.solution = solution;
    this.threadsController = threadsController;
  }

  public void run() throws Exception {
    File inputFile = threadsController.getSpreadsheetReader().getInputFile();
    String pathToOutputFile =
        inputFile.getAbsoluteFile().getParent()
            + File.separator
            + inputFile.getName().split("\\.")[0]
            + "_output.ods";

    threadsController.println("output file: " + pathToOutputFile);

    String[][] output = getOutput();
    saveOutput(pathToOutputFile, output);
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

  private void saveOutput(String pathToOutputFile, String[][] output) throws Exception {
    File outputFile = new File(pathToOutputFile);
    if (outputFile.exists()) {
      throw new Exception("The output file already exists and will not be overwritten");
    }
    Sheet sheet = threadsController.getSpreadsheetReader().getSheet();
    Range range = sheet.getRange("B6:AF" + Config.LAST_ROW_OF_SCHEDULE);
    range.setValues(output);
    SpreadSheet spreadSheet = threadsController.getSpreadsheetReader().getSpreadSheet();
    spreadSheet.save(outputFile);
  }
}
