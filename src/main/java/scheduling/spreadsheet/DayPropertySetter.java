package scheduling.spreadsheet;

@FunctionalInterface
interface DayPropertySetter<Value> {
	void set(int day, Value value);
}
