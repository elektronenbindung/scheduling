package scheduling.matching;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.MatchingAlgorithm.Matching;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import scheduling.common.Config;
import scheduling.common.ThreadsController;
import scheduling.common.Solution;

public class ShiftMatching {

	private final ThreadsController threadsController;
	private final Graph<Vertex, DefaultWeightedEdge> graph;
	private final Set<Vertex> daysSet;
	private final Set<Vertex> employeesSet;
	private final Day[] days;

	public ShiftMatching(ThreadsController threadsController) {
		this.threadsController = threadsController;
		graph = GraphTypeBuilder.undirected().allowingMultipleEdges(false).allowingSelfLoops(false)
				.edgeClass(DefaultWeightedEdge.class).vertexClass(Vertex.class).weighted(true).buildGraph();
		daysSet = new HashSet<>();
		employeesSet = new HashSet<>();
		days = new Day[threadsController.getSpreadsheetReader().getLengthOfMonth()];
	}

	public Solution run() {
		addDaysToGraph();
		addEmployeesToGraph();
		Set<DefaultWeightedEdge> matchingResult = performMatching();
		return getSolutionFromMatching(matchingResult);
	}

	private void addDaysToGraph() {
		for (int dayNumber = 0; dayNumber < days.length; dayNumber++) {
			days[dayNumber] = new Day(dayNumber);
			daysSet.add(days[dayNumber]);
			graph.addVertex(days[dayNumber]);
		}
	}

	private void addEmployeesToGraph() {
		for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
			addEmployeeToGraph(employee);
		}
	}

	private void addEmployeeToGraph(int employee) {
		for (int shiftNumber = 0; shiftNumber < Math.floor(threadsController.getSpreadsheetReader()
				.getDaysToWorkInTotalForEmployee(employee)); shiftNumber++) {
			Shift shift = new Shift(employee, shiftNumber);
			employeesSet.add(shift);
			graph.addVertex(shift);
			addEdgesForShift(shift, employee, shiftNumber);
		}
	}

	private void addEdgesForShift(Shift shift, int employee, int shiftNumber) {
		int weightForFreeDay = getWeightForFreeDay(shiftNumber, employee);
		for (int day = 0; day < days.length; day++) {
			if (threadsController.getSpreadsheetReader().isEmployeeAvailableOnDay(employee, day)) {
				int edgeWeight = getEdgeWeight(weightForFreeDay, day, employee);
				graph.addEdge(shift, days[day]);
				graph.setEdgeWeight(shift, days[day], edgeWeight);
			}
		}
	}

	private int getWeightForFreeDay(int shiftNumber, int employee) {
		return shiftNumber < threadsController.getSpreadsheetReader()
				.getDaysToWorkAtFreeDayForEmployee(employee)
				? Config.WEIGHT_FOR_FREE_DAY
				: Config.WEIGHT_FOR_NORMAL_DAY;
	}

	private int getEdgeWeight(int weightForFreeDay, int day, int employee) {
		int edgeWeight = threadsController.getSpreadsheetReader().isFreeDay(day)
				? weightForFreeDay
				: Config.WEIGHT_FOR_NORMAL_DAY;
		edgeWeight = threadsController.getSpreadsheetReader().getEmployeeOnFixedDay(day) == employee
				? edgeWeight + Config.WEIGHT_FOR_FIXED_DAY
				: edgeWeight;
		return edgeWeight;
	}

	private Set<DefaultWeightedEdge> performMatching() {
		MaximumWeightBipartiteMatching<Vertex, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<>(
				graph, employeesSet, daysSet);

		Matching<Vertex, DefaultWeightedEdge> result = matching.getMatching();

		threadsController.informAboutSolvabilityOfSchedule(result.isPerfect());

		return result.getEdges();
	}

	private Solution getSolutionFromMatching(Set<DefaultWeightedEdge> matchingResult) {
		int[] solution = new int[threadsController.getSpreadsheetReader().getLengthOfMonth()];
		Arrays.fill(solution, Config.MISSING_EMPLOYEE);

		matchingResult.forEach(edge -> {
			Shift shift = (Shift) graph.getEdgeSource(edge);
			Day day = (Day) graph.getEdgeTarget(edge);
			int employee = shift.employee();
			int dayNumber = day.dayNumber();
			solution[dayNumber] = employee;
		});

		int[] numberOfFreeDaysForEmployee = getNumberOfFreeDaysForEmployee(solution);
		return new Solution(solution, numberOfFreeDaysForEmployee, threadsController.getSpreadsheetReader());
	}

	private int[] getNumberOfFreeDaysForEmployee(int[] solution) {
		int[] numberOfFreeDaysForEmployee = new int[Config.NUMBER_OF_EMPLOYEES];

		for (int day = 0; day < threadsController.getSpreadsheetReader().getLengthOfMonth(); day++) {
			if (threadsController.getSpreadsheetReader().isFreeDay(day)) {
				if (solution[day] != Config.MISSING_EMPLOYEE) {
					numberOfFreeDaysForEmployee[solution[day]]++;
				}
			}
		}
		return numberOfFreeDaysForEmployee;
	}
}