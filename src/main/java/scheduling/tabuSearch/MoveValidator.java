package scheduling.tabuSearch;

import scheduling.common.Config;
import scheduling.common.Solution;
import scheduling.spreadsheet.SpreadsheetReader;

public class MoveValidator {

	private final SpreadsheetReader spreadsheetReader;

	public MoveValidator(SpreadsheetReader spreadsheetReader) {
		this.spreadsheetReader = spreadsheetReader;
	}

	public boolean isMoveForbidden(Solution currentSolution, Move move) {
		if(isAtLeastOneEmployeeFixed(currentSolution, move)){
			return true;
		}

		if(isAtLeastOneEmployeeUnavailable(currentSolution, move)){
			return true;
		}

		return areFreeDaysForbidden(currentSolution, move);
	}

	private boolean areFreeDaysForbidden(Solution currentSolution, Move move) {
		boolean isFromDayFree = spreadsheetReader.isFreeDay(move.fromDay());
		boolean isToDayFree = spreadsheetReader.isFreeDay(move.toDay());

		if (isFromDayFree == isToDayFree) {
			return false;
		}

		int employeeGainingFreeDay;
		int employeeLosingFreeDay;

		if (isToDayFree) {
			employeeGainingFreeDay = currentSolution.getEmployeeForDay(move.fromDay());
			employeeLosingFreeDay = currentSolution.getEmployeeForDay(move.toDay());
		} else {
			employeeGainingFreeDay = currentSolution.getEmployeeForDay(move.toDay());
			employeeLosingFreeDay = currentSolution.getEmployeeForDay(move.fromDay());
		}

		int freeDaysForGainingEmployee = currentSolution.getNumberOfFreeDaysForEmployee(employeeGainingFreeDay);
		boolean canGain = (employeeGainingFreeDay == Config.MISSING_EMPLOYEE)
				|| (freeDaysForGainingEmployee < spreadsheetReader.getDaysToWorkAtFreeDayForEmployee(employeeGainingFreeDay));

		int freeDaysForLosingEmployee = currentSolution.getNumberOfFreeDaysForEmployee(employeeLosingFreeDay);
		boolean canLose = (employeeLosingFreeDay == Config.MISSING_EMPLOYEE)
				|| (freeDaysForLosingEmployee > spreadsheetReader.getDaysToWorkAtFreeDayForEmployee(employeeLosingFreeDay));

		return !canGain && !canLose;
	}

	private boolean isAtLeastOneEmployeeUnavailable(Solution currentSolution, Move move) {
		int employee1 = currentSolution.getEmployeeForDay(move.fromDay());
		int employee2 = currentSolution.getEmployeeForDay(move.toDay());

		boolean employee1AvailableAtToDay = spreadsheetReader.isEmployeeAvailableOnDay(employee1, move.toDay());
		boolean employee2AvailableAtFromDay = spreadsheetReader.isEmployeeAvailableOnDay(employee2, move.fromDay());

		return !(employee1AvailableAtToDay && employee2AvailableAtFromDay);
	}

	private boolean isAtLeastOneEmployeeFixed(Solution currentSolution, Move move) {
		return isEmployeeFixedOnDay(currentSolution, move.fromDay())
				|| isEmployeeFixedOnDay(currentSolution, move.toDay());
	}

	private boolean isEmployeeFixedOnDay(Solution currentSolution, int day) {
		int fixedEmployee = spreadsheetReader.getEmployeeOnFixedDay(day);
		if (fixedEmployee == Config.MISSING_EMPLOYEE) {
			return false;
		}
		return currentSolution.getEmployeeForDay(day) == fixedEmployee;
}}
