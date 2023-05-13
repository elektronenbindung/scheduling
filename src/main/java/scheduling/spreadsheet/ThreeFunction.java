package scheduling.spreadsheet;

@FunctionalInterface
interface ThreeFunction<One, Two, Three, Four> {
    public Four apply(One one, Two two, Three three);
}
