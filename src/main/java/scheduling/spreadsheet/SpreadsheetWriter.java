package scheduling.spreadsheet;

import java.io.File;
import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;

import scheduling.common.Config;
import scheduling.common.Controller;
import scheduling.common.Solution;

public class SpreadsheetWriter {
    private SpreadsheetReader input;
    private Solution solution;
    private Controller controller;

    public SpreadsheetWriter(SpreadsheetReader input, Solution solution, Controller controller) {
        this.input = input;
        this.solution = solution;
        this.controller = controller;
    }

    public void run() throws Exception {
        File inputFile = input.getInputFile();
        String pathToOutputFile = inputFile.getAbsoluteFile().getParent() + File.separator
                + inputFile.getName().split("\\.")[0] + "_output.ods";

        controller.println("output file: " + pathToOutputFile);

        String[][] output = getOutput();
        saveOutput(pathToOutputFile, output);

    }

    private String[][] getOutput() {
        String[][] output = new String[Config.NUMBER_OF_EMPLOYEES][31];

        for (int day = 0; day < input.getLengthOfMonth(); day++) {
            int employee = solution.getEmployeeForDay(day);
            if (employee != -1) {
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
        Sheet sheet = input.getSheet();
        Range range = sheet.getRange("B6:AF" + Config.LAST_ROW_OF_SCHEDULE);
        range.setValues(output);
        SpreadSheet spreadSheet = input.getSpreadSheet();
        spreadSheet.save(outputFile);
    }
}
