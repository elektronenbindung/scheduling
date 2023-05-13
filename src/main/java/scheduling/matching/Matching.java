package scheduling.matching;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;

import scheduling.common.Config;
import scheduling.common.Solution;
import scheduling.spreadsheet.SpreadsheetReader;

public class Matching {

    private SpreadsheetReader inputReader;

    public Matching(SpreadsheetReader inputReader) {
        this.inputReader = inputReader;
    }

    public Solution run() {
        Graph<Vertex, DefaultWeightedEdge> graph = GraphTypeBuilder
                .undirected()
                .allowingMultipleEdges(false)
                .allowingSelfLoops(false)
                .edgeClass(DefaultWeightedEdge.class)
                .vertexClass(Vertex.class)
                .weighted(true)
                .buildGraph();
        Set<Vertex> daysSet = new HashSet<Vertex>();
        Set<Vertex> workersSet = new HashSet<Vertex>();
        Day[] days = new Day[inputReader.getLengthOfMonth()];

        for (int dayNumber = 0; dayNumber < days.length; dayNumber++) {
            days[dayNumber] = new Day(dayNumber);
            daysSet.add(days[dayNumber]);
            graph.addVertex(days[dayNumber]);
        }
        for (int employee = 0; employee < Config.NUMBER_OF_EMPLOYEES; employee++) {
            for (int shift = 0; shift < inputReader.getDaysToWorkInTotalForEmployee(employee); shift++) {
                Worker worker = new Worker(employee);
                workersSet.add(worker);
                graph.addVertex(worker);
                int weightForFreeDay = shift < inputReader.getDaysToWorkAtFreeDayForEmployee(employee)
                        ? Config.WEIGHT_FOR_FREE_DAY
                        : Config.WEIGHT_FOR_NORMAL_DAY;
                for (int day = 0; day < days.length; day++) {
                    if (inputReader.getIsEmployeeAvailableOnDay(employee, day)) {
                        int weight = inputReader.isFreeDay(day) ? weightForFreeDay : Config.WEIGHT_FOR_NORMAL_DAY;
                        weight = inputReader.getEmployeeOnFixedDay(day) == employee
                                ? weight + Config.WEIGHT_FOR_FIXED_DAY
                                : weight;
                        graph.addEdge(worker, days[day]);
                        graph.setEdgeWeight(worker, days[day], weight);
                    }
                }
            }
        }
        MaximumWeightBipartiteMatching<Vertex, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<Vertex, DefaultWeightedEdge>(
                graph, workersSet, daysSet);
        Set<DefaultWeightedEdge> matchingResult = matching.getMatching().getEdges();
        Iterator<DefaultWeightedEdge> iterator = matchingResult.iterator();
        int[] solution = new int[inputReader.getLengthOfMonth()];

        Arrays.fill(solution, -1);

        while (iterator.hasNext()) {
            DefaultWeightedEdge edge = iterator.next();
            Worker worker = (Worker) graph.getEdgeSource(edge);
            Day day = (Day) graph.getEdgeTarget(edge);
            int employeeNumber = worker.getEmployeeNumber();
            int dayNumber = day.getDayNumber();
            solution[dayNumber] = employeeNumber;
        }
        return new Solution(solution, inputReader);
    }

}
