package scheduling;

import java.io.File;
import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;

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
        File outputFile = new File(pathToOutputFile);

        controller.println("output file: " + pathToOutputFile);

        if (outputFile.exists()) {
            throw new Exception("The output file already exists and will not be overwritten");
        }
        Sheet sheet = input.getSheet();
        String[][] output = new String[Config.NUMBER_OF_EMPLOYEES][31];

        for (int day = 0; day < input.getLengthOfMonth(); day++) {
            int employee = solution.getEmployeeForDay(day);
            if (employee != -1) {
                output[employee][day] = Config.WORKING;
            }
        }
        int numberOfRows = 5 + Config.NUMBER_OF_EMPLOYEES;
        Range range = sheet.getRange("B6:AF" + numberOfRows);
        range.setValues(output);
        SpreadSheet spreadSheet = input.getSpreadSheet();
        spreadSheet.save(outputFile);

    }
}
