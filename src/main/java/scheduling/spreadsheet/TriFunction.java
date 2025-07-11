package scheduling.spreadsheet;

@FunctionalInterface
interface TriFunction<One, Two, Three, Four> {
	Four apply(One one, Two two, Three three);
}
