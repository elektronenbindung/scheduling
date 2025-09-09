package scheduling.common;

public class Config {
	public static final int NUMBER_OF_EMPLOYEES = 30;
	public static final int NUMBER_OF_PARALLEL_THREADS = 50;
	public static final int ROWS_OF_HEADER = 5;
	public static final int LAST_ROW_OF_SCHEDULE = ROWS_OF_HEADER + NUMBER_OF_EMPLOYEES;
	public static final String WORK_DAY = "A";
	public static final String WORKING = "x";
	public static final String SINGLE_SHIFT = "E";
	public static final int MISSING_EMPLOYEE = -1;
	public static final int WEIGHT_FOR_NORMAL_DAY = 1;
	public static final int WEIGHT_FOR_FIXED_DAY = 1000;
	public static final int WEIGHT_FOR_FREE_DAY = 32;
	public static final int MAX_RETRIES_OF_TABU_SEARCH = 30000;
	public static final int TABU_SEARCH_NEIGHBORHOOD_SAMPLE_SIZE = 100;
	public static final int LENGTH_OF_TABU_LIST = 15;
	public static final int MAX_RETRIES_OF_SOLUTION = 2000;
	public static final int LENGTH_OF_SOLUTION_LIST = 20;
	public static final int DEFAULT_MAX_LENGTH_OF_SHIFT = 31;
	public static final int INTERVAL_FOR_ONE_DAY = 2;
	public static final double PENALTY_FOR_FORBIDDEN_SHIFT = 10000;
	public static final double PENALTY_FOR_MANDATORY_BLOCK_SHIFT = 300;
	public static final double OPTIMAL_SOLUTION = 0;
	public static final String QUIT = ":q";
	public static final String VERSION = "--version";

	private Config() {
	}
}
