package scheduling.spreadsheet;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;

import scheduling.common.Config;

public class SpreadsheetReader {
	private final File input;
	private final SpreadSheetReaderTools tools;
	private SpreadSheet spreadSheet;
	private Sheet sheet;
	private int lengthOfMonth;
	private double[] maxLengthOfShiftPerEmployee;
	private Boolean[] isFreeDay;
	private Boolean[] isSingleSShiftForbiddenOnDay;
	private Integer[] fixedEmployeeOnDay;
	private Boolean[][] isAvailablePerDay;
	private double[] additionalFreeDaysBetweenShifts;
	private double[] daysToWorkInTotalPerEmployee;
	private double[] daysToWorkAtFreeDayPerEmployee;
	private double[] wishedLengthOfShiftPerEmployee;
	private double[] expectedDaysBetweenShiftsPerEmployee;

	public SpreadsheetReader(File inpFile) {
		this.input = Objects.requireNonNull(inpFile, "Input file must not be null");
		this.tools = new SpreadSheetReaderTools(this);
	}

	public SpreadSheet getSpreadSheet() {
		return spreadSheet;
	}

	public Sheet getSheet() {
		return sheet;
	}

	public File getInputFile() {
		return input;
	}

	public double getMaxLengthOfShiftPerEmployee(int employee) {
		return maxLengthOfShiftPerEmployee[employee];
	}

	public double getWishedLengthOfShiftForEmployee(int employee) {
		return wishedLengthOfShiftPerEmployee[employee];
	}

	public double getExpectedDaysBetweenShiftsForEmployee(int employee) {
		return expectedDaysBetweenShiftsPerEmployee[employee];
	}

	public int getLengthOfMonth() {
		return lengthOfMonth;
	}

	public boolean isFreeDay(int day) {
		return isFreeDay[day];
	}

	public int getEmployeeOnFixedDay(int day) {
		return fixedEmployeeOnDay[day];
	}

	public boolean isEmployeeAvailableOnDay(int employee, int day) {
		if (employee == Config.MISSING_EMPLOYEE) {
			return true;
		}
		return isAvailablePerDay[employee][day];
	}

	public double getAdditionalFreeDaysBetweenShifts(int employee) {
		return additionalFreeDaysBetweenShifts[employee];
	}

	public double getDaysToWorkInTotalForEmployee(int employee) {
		return daysToWorkInTotalPerEmployee[employee];
	}

	public double getDaysToWorkAtFreeDayForEmployee(int employee) {
		return daysToWorkAtFreeDayPerEmployee[employee];
	}

	public boolean isSingleShiftForbiddenOnDay(int day) {
		return isSingleSShiftForbiddenOnDay[day];
	}

	public void run() throws IOException {
		spreadSheet = new SpreadSheet(input);
		sheet = spreadSheet.getSheet(0);
		lengthOfMonth = tools.calculateLengthOfMonth();
		isFreeDay = tools.calculateIsFreeDay();
		isSingleSShiftForbiddenOnDay = tools.calculateIsSingleShiftForbiddenOnDay();
		additionalFreeDaysBetweenShifts = tools.calculateAdditionalFreeDaysBetweenShifts();
		daysToWorkInTotalPerEmployee = tools.calculateDaysToWorkInTotal();
		maxLengthOfShiftPerEmployee = tools.calculateMaxLengthOfShiftPerEmployee();
		isAvailablePerDay = tools.calculateAvailability();
		fixedEmployeeOnDay = tools.calculateFixedEmployees();
		daysToWorkAtFreeDayPerEmployee = tools.calculateDaysToWorkAtFreeDay();
		wishedLengthOfShiftPerEmployee = tools.calculateWishedLengthOfShiftPerEmployee();
		expectedDaysBetweenShiftsPerEmployee = tools.calculateExpectedDaysBetweenShifts();
	}
}
