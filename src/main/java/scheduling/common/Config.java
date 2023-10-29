package scheduling.common;

public class Config {
    public final static int NUMBER_OF_EMPLOYEES = 30;
    public final static int ROWS_OF_HEADER = 5;
    public final static int LAST_ROW_OF_SCHEDULE = ROWS_OF_HEADER + NUMBER_OF_EMPLOYEES;
    public final static String FREE_DAY = "F";
    public final static String WORKING = "x";
    public final static String SINGLE_SHIFT = "E";
    public final static int MISSING_EMPLOYEE = -1;
    public final static int WEIGHT_FOR_NORMAL_DAY = 1;
    public final static int WEIGHT_FOR_FIXED_DAY = 1000;
    public final static int WEIGHT_FOR_FREE_DAY = 32;
    public final static int MAX_RETRIES_OF_TABU_SEARCH = 23000000;
    public final static int RETRIES_OF_INVALID_SOLUTION = 5;
    public final static int LENGTH_OF_TABU_LIST = 15;
    public final static int MAX_RETRIES_OF_SOLUTION = 700000;
    public final static int LENGTH_OF_SOLUTION_LIST = 30;
    public final static int DEFAULT_MAX_LENGTH_OF_SHIFT = 31;
    public final static double PENALTY_FOR_FORBIDDEN_SHIFT = 10000;
    public final static double PENALTY_FOR_UNWANTED_SHIFT = 300;
    public final static String QUIT = ":q";
    public final static String VERSION = "--version";

    private Config() {

    }
}
