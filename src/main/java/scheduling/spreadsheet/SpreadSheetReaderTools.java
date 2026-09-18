package scheduling.spreadsheet;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntToDoubleFunction;
import java.util.stream.IntStream;

import com.github.miachm.sods.Range;
import scheduling.common.Config;

public class SpreadSheetReaderTools {
	private final SpreadsheetReader reader;
	private final SpreadsheetReaderToolsHelper helper;
	private static final String MAX_LENGTH_OF_SHIFT_COLUMN = "AK";
	private static final String DAYS_TO_WORK_IN_TOTAL_COLUMN = "AH";
	private static final String DAYS_TO_WORK_AT_FREE_DAY_COLUMN = "AI";
	private static final String WISHED_LENGTH_OF_SHIFT_COLUMN = "AJ";
	private static final String ADDITIONAL_MIN_FREE_DAYS_BETWEEN_SHIFTS = "AL";
	private static final String DATE_CELL = "L1";
	private static final int FREE_DAY_ROW_OFFSET = 1;
	private static final int SINGLE_SHIFT_ALLOWED_ROW_OFFSET = 3;

	public SpreadSheetReaderTools(SpreadsheetReader reader) {
		this.reader = Objects.requireNonNull(reader, "SpreadsheetReader must not be null");
		this.helper = new SpreadsheetReaderToolsHelper(reader);
	}

	public double[] calculateMaxLengthOfShiftPerEmployee() {
		double[] result = helper.calculateEmployeePreferencesOnSpreadsheet(MAX_LENGTH_OF_SHIFT_COLUMN, false);
		IntToDoubleFunction function = employee -> {
			double maxLengthOfShift = result[employee] > 0 ? result[employee] : Config.DEFAULT_MAX_LENGTH_OF_SHIFT;
			return Math.min(maxLengthOfShift, reader.getDaysToWorkInTotalForEmployee(employee));
		};
		return IntStream.range(0, result.length).mapToDouble(function).toArray();
	}

	public double[] calculateExpectedDaysBetweenShifts() {
		double[] expectedDaysBetweenShiftsPerEmployee = new double[Config.NUMBER_OF_EMPLOYEES];

		for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
			if (reader.getDaysToWorkInTotalForEmployee(employee) <= 0
					|| reader.getWishedLengthOfShiftForEmployee(employee) <= 0) {
				expectedDaysBetweenShiftsPerEmployee[employee] = Config.MISSING_EMPLOYEE;
			} else {
				expectedDaysBetweenShiftsPerEmployee[employee] = (reader.getLengthOfMonth()
						* reader.getWishedLengthOfShiftForEmployee(employee))
						/ reader.getDaysToWorkInTotalForEmployee(employee);
			}
		}
		return expectedDaysBetweenShiftsPerEmployee;
	}

	public int calculateLengthOfMonth() {
		Range range = reader.getSheet().getRange(DATE_CELL);
		Object value = range.getValue();
		if (value instanceof LocalDate) {
			return ((LocalDate) value).lengthOfMonth();
		} else {
			throw new IllegalArgumentException("Cell " + DATE_CELL + " does not contain a valid date (LocalDate).");
		}
	}

	public boolean[] calculateIsFreeDay() {
		int row = Config.LAST_ROW_OF_SCHEDULE + FREE_DAY_ROW_OFFSET;
		return negateDayProperty(helper.calculateDayProperty(row, Config.WORK_DAY));
	}

	public boolean[] calculateIsSingleShiftForbiddenOnDay() {
		int row = Config.LAST_ROW_OF_SCHEDULE + SINGLE_SHIFT_ALLOWED_ROW_OFFSET;
		return negateDayProperty(helper.calculateDayProperty(row, Config.SINGLE_SHIFT));
	}

	public int[] calculateFixedEmployees() {
		int[] result = new int[reader.getLengthOfMonth()];
		Arrays.fill(result, Config.MISSING_EMPLOYEE);
		helper.setFixedEmployeeOnDay(result);

		for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
			helper.calculatePropertyForEmployeeOnDays(employee, helper.getFunctionForCalculationOfFixedEmployees(),
					(day, fixedEmployee) -> result[day] = fixedEmployee);
		}
		return result;
	}

	public boolean[][] calculateAvailability() {
		boolean[][] result = new boolean[Config.NUMBER_OF_EMPLOYEES][reader.getLengthOfMonth()];
		for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
			boolean[] resultForEmployee = result[employee];
			helper.calculatePropertyForEmployeeOnDays(employee, helper.getFunctionForCalculationOfAvailableEmployees(),
					(day, isAvailable) -> resultForEmployee[day] = isAvailable);
		}
		return result;
	}

	public boolean[][] calculateWishedShift() {
		boolean[][] result = new boolean[Config.NUMBER_OF_EMPLOYEES][reader.getLengthOfMonth()];

		for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
			boolean[] resultForEmployee = result[employee];
			helper.calculatePropertyForEmployeeOnDays(employee, helper.getFunctionForWishedShift(),
					(day, isWished) -> resultForEmployee[day] = isWished);
		}
		return result;
	}

	public boolean[][] calculateAvoidedShift() {
		boolean[][] result = new boolean[Config.NUMBER_OF_EMPLOYEES][reader.getLengthOfMonth()];

		for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
			boolean[] resultForEmployee = result[employee];
			helper.calculatePropertyForEmployeeOnDays(employee, helper.getFunctionForAvoidedShift(),
					(day, isAvoided) -> resultForEmployee[day] = isAvoided);
		}
		return result;
	}

	public double[] calculateAdditionalFreeDaysBetweenShifts() {
		return Arrays
				.stream(helper.calculateEmployeePreferencesOnSpreadsheet(ADDITIONAL_MIN_FREE_DAYS_BETWEEN_SHIFTS, true))
				.map(v -> v == Config.MISSING_EMPLOYEE ? 0 : v).toArray();
	}

	public double[] calculateDaysToWorkInTotal() {
		return helper.calculateEmployeePreferencesOnSpreadsheet(DAYS_TO_WORK_IN_TOTAL_COLUMN, false);
	}

	public double[] calculateDaysToWorkAtFreeDay() {
		return helper.calculateEmployeePreferencesOnSpreadsheet(DAYS_TO_WORK_AT_FREE_DAY_COLUMN, false);
	}

	public double[] calculateWishedLengthOfShiftPerEmployee() {
		double[] result = helper.calculateEmployeePreferencesOnSpreadsheet(WISHED_LENGTH_OF_SHIFT_COLUMN, false);

		return IntStream.range(0, result.length)
				.mapToDouble(employee -> Math.min(result[employee], reader.getMaxLengthOfShiftPerEmployee(employee)))
				.toArray();
	}

	private boolean[] negateDayProperty(boolean[] dayProperty) {
		boolean[] result = new boolean[dayProperty.length];
		for (int day = 0; day < dayProperty.length; day++) {
			result[day] = !dayProperty[day];
		}
		return result;
	}

}
