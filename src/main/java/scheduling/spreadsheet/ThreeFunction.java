package scheduling.spreadsheet;

@FunctionalInterface
interface ThreeFunction<One, Two, Three, Four> {
  Four apply(One one, Two two, Three three);
}
