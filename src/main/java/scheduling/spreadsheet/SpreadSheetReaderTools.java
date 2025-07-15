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
	private static final String DATE_CELL = "L1";
	private static final String SCHEDULE_DATA_START_COLUMN = "B";
	private static final String SCHEDULE_DATA_END_COLUMN = "AF";

	private static final int FREE_DAY_ROW_OFFSET = 1;
	private static final int SINGLE_SHIFT_ALLOWED_ROW_OFFSET = 3;
	private static final int SCHEDULE_DATA_START_ROW = 6;

	public SpreadSheetReaderTools(SpreadsheetReader reader) {
		this.reader = Objects.requireNonNull(reader, "SpreadsheetReader must not be null");
		this.helper = new SpreadsheetReaderToolsHelper(reader);
	}

	public double[] calculateMaxLengthOfShiftPerEmployee() {
		double[] result = calculateEmployeePreferencesOnSpreadsheet(MAX_LENGTH_OF_SHIFT_COLUMN);
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

	public Boolean[] calculateIsFreeDay() {
		int row = Config.LAST_ROW_OF_SCHEDULE + FREE_DAY_ROW_OFFSET;
		return Arrays.stream(calculateDayProperty(row, Config.WORK_DAY)).map(x -> !x).toArray(Boolean[]::new);
	}

	public Boolean[] calculateIsSingleShiftAllowedOnDay() {
		int row = Config.LAST_ROW_OF_SCHEDULE + SINGLE_SHIFT_ALLOWED_ROW_OFFSET;
		return calculateDayProperty(row, Config.SINGLE_SHIFT);
	}

	public Integer[] calculateFixedEmployees() {
		Integer[] result = new Integer[reader.getLengthOfMonth()];
		Arrays.fill(result, Config.MISSING_EMPLOYEE);
		helper.setFixedEmployeeOnDay(result);

		for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
			calculatePropertyForEmployeeOnDays(result, employee, helper.getFunctionForCalculationOfFixedEmployees());
		}
		return result;
	}

	public Boolean[][] calculateAvailability() {
		Boolean[][] result = new Boolean[Config.NUMBER_OF_EMPLOYEES][reader.getLengthOfMonth()];
		for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
			calculatePropertyForEmployeeOnDays(result[employee], employee,
					helper.getFunctionForCalculationOfAvailableEmployees());
		}
		return result;
	}

	public double[] calculateDaysToWorkInTotal() {
		return calculateEmployeePreferencesOnSpreadsheet(DAYS_TO_WORK_IN_TOTAL_COLUMN);
	}

	public double[] calculateDaysToWorkAtFreeDay() {
		return calculateEmployeePreferencesOnSpreadsheet(DAYS_TO_WORK_AT_FREE_DAY_COLUMN);
	}

	public double[] calculateWishedLengthOfShiftPerEmployee() {
		double[] result = calculateEmployeePreferencesOnSpreadsheet(WISHED_LENGTH_OF_SHIFT_COLUMN);

		return IntStream.range(0, result.length)
				.mapToDouble(employee -> Math.min(result[employee], reader.getMaxLengthOfShiftPerEmployee(employee)))
				.toArray();
	}

	private Boolean[] calculateDayProperty(int row, String property) {
		Boolean[] dayProperty = new Boolean[reader.getLengthOfMonth()];
		String a1Notation = SCHEDULE_DATA_START_COLUMN + row + ":" + SCHEDULE_DATA_END_COLUMN + row;
		Range range = reader.getSheet().getRange(a1Notation);
		Object[] objects = range.getValues()[0];

		for (int index = 0; index < reader.getLengthOfMonth(); index++) {
			String str = String.valueOf(objects[index]);
			dayProperty[index] = str.equals(property);
		}

		return dayProperty;
	}

	private double[] calculateEmployeePreferencesOnSpreadsheet(String columnInA1Notation) {
		String a1Notation = columnInA1Notation + SCHEDULE_DATA_START_ROW + ":" + columnInA1Notation
				+ Config.LAST_ROW_OF_SCHEDULE;
		double[] preferencesPerEmployee = new double[Config.NUMBER_OF_EMPLOYEES];
		Range range = reader.getSheet().getRange(a1Notation);
		Object[][] values = range.getValues();

		for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
			Object[] value = values[employee];
			preferencesPerEmployee[employee] = value[0] != null
					? Double.parseDouble(String.valueOf(value[0]))
					: Config.MISSING_EMPLOYEE;
			preferencesPerEmployee[employee] = preferencesPerEmployee[employee] < 0
					? Config.MISSING_EMPLOYEE
					: preferencesPerEmployee[employee];
		}
		return preferencesPerEmployee;
	}

	private <T> void calculatePropertyForEmployeeOnDays(T[] result, int employee,
			TriFunction<Range, Integer, Integer, T> function) {
		String a1Notation = SCHEDULE_DATA_START_COLUMN + SCHEDULE_DATA_START_ROW + ":" + SCHEDULE_DATA_END_COLUMN
				+ Config.LAST_ROW_OF_SCHEDULE;
		Range range = reader.getSheet().getRange(a1Notation);

		for (int day = 0; day < reader.getLengthOfMonth(); day++) {
			result[day] = function.apply(range, employee, day);
		}
	}
}
