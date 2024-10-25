package scheduling.matching;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.MatchingAlgorithm.Matching;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import scheduling.common.Config;
import scheduling.common.ThreadsController;
import scheduling.common.Solution;
import scheduling.spreadsheet.SpreadsheetReader;

public class ScheduleMatching {

    private ThreadsController controller;
    private SpreadsheetReader inputReader;
    private Graph<Vertex, DefaultWeightedEdge> graph;
    private Set<Vertex> daysSet;
    private Set<Vertex> employeesSet;
    private Day[] days;

    public ScheduleMatching(SpreadsheetReader inputReader, ThreadsController controller) {
        this.controller = controller;
        this.inputReader = inputReader;
        graph = GraphTypeBuilder
                .undirected()
                .allowingMultipleEdges(false)
                .allowingSelfLoops(false)
                .edgeClass(DefaultWeightedEdge.class)
                .vertexClass(Vertex.class)
                .weighted(true)
                .buildGraph();
        daysSet = new HashSet<Vertex>();
        employeesSet = new HashSet<Vertex>();
        days = new Day[inputReader.getLengthOfMonth()];
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
            for (int shiftNumber = 0; shiftNumber < Math.floor(inputReader
                    .getDaysToWorkInTotalForEmployee(employee)); shiftNumber++) {
                Shift shift = new Shift(employee);
                employeesSet.add(shift);
                graph.addVertex(shift);
                int weightForFreeDay = shiftNumber < inputReader.getDaysToWorkAtFreeDayForEmployee(employee)
                        ? Config.WEIGHT_FOR_FREE_DAY
                        : Config.WEIGHT_FOR_NORMAL_DAY;
                for (int day = 0; day < days.length; day++) {
                    if (inputReader.getIsEmployeeAvailableOnDay(employee, day)) {
                        int weight = inputReader.isFreeDay(day) ? weightForFreeDay : Config.WEIGHT_FOR_NORMAL_DAY;
                        weight = inputReader.getEmployeeOnFixedDay(day) == employee
                                ? weight + Config.WEIGHT_FOR_FIXED_DAY
                                : weight;
                        graph.addEdge(shift, days[day]);
                        graph.setEdgeWeight(shift, days[day], weight);
                    }
                }
            }
        }
    }

    private Set<DefaultWeightedEdge> performMatching() {
        MaximumWeightBipartiteMatching<Vertex, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<Vertex, DefaultWeightedEdge>(
                graph, employeesSet, daysSet);

        Matching<Vertex, DefaultWeightedEdge> result = matching.getMatching();

        if (!result.isPerfect()) {
            controller.println("Warning: No perfect matching between shift and days has been found");
        }

        return result.getEdges();
    }

    private Solution getSolutionFromMatching(Set<DefaultWeightedEdge> matchingResult) {
        Iterator<DefaultWeightedEdge> iterator = matchingResult.iterator();
        int[] solution = new int[inputReader.getLengthOfMonth()];

        Arrays.fill(solution, Config.MISSING_EMPLOYEE);

        while (iterator.hasNext()) {
            DefaultWeightedEdge edge = iterator.next();
            Shift shift = (Shift) graph.getEdgeSource(edge);
            Day day = (Day) graph.getEdgeTarget(edge);
            int employee = shift.getEmployee();
            int dayNumber = day.getDayNumber();
            solution[dayNumber] = employee;
        }
        int[] numberOfFreeDaysForEmployee = getNumberOfFreeDaysForEmployee(solution);
        return new Solution(solution, numberOfFreeDaysForEmployee, inputReader);
    }

    private int[] getNumberOfFreeDaysForEmployee(int[] solution) {
        int[] numberOfFreeDaysForEmployee = new int[Config.NUMBER_OF_EMPLOYEES];

        for (int day = 0; day < inputReader.getLengthOfMonth(); day++) {
            if (inputReader.isFreeDay(day)) {
                numberOfFreeDaysForEmployee[solution[day]]++;
            }
        }
        return numberOfFreeDaysForEmployee;
    }

}
