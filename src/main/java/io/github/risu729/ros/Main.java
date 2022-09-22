import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.TreeMap;

public final class Main {

  private static final Path SETTINGS_PATH = Path.of("src", "main", "resources", "settings.properties");
  private static final Path RESULTS_DIR = Path.of("results");
  
  private static long lastPrinted = System.currentTimeMillis();
  
  public static void main(String[] args) throws IOException {
    
    var properties = new Properties();
    try (var input = Files.newInputStream(SETTINGS_PATH)) {
      properties.load(input);
    }
    
    int[] nRange = loadRange(properties, "n");
    int[] mRange = loadRange(properties, "m");
    int[] trialsRange = loadRange(properties, "trials");

    List<Variables> variablesList = new LinkedList<>();
    for (int n = nRange[0]; n <= nRange[1]; n++) {
      for (int m = mRange[0]; m <= mRange[1]; m++) {
        for (int trials = trialsRange[0]; trials <= trialsRange[1]; trials++) {
          variablesList.add(new Variables(n, m, trials));
        }
      }
    }
    
    printTime("Finished variables list creation", TimeUnit.MILLISECONDS);

    Map<Variables, Double> result = variablesList.parallelStream()
        .collect(Collectors.toMap(UnaryOperator.identity(), Main::execute, (e1, e2) -> e1, TreeMap::new));
    
    printTime("Finished exection", TimeUnit.MINUTES);
    
    var resultsPath = RESULTS_DIR.resolve(OffsetDateTime.now(ZoneOffset.UTC).toString().replace(":", "") + ".csv");
    Files.createDirectories(RESULTS_DIR);
    Files.createFile(resultsPath);
    try (BufferedWriter writer = Files.newBufferedWriter(resultsPath)) {
      writer.write(String.join(",", "n", "m", "trials", "result"));
      writer.newLine();
      result.forEach((key, value) -> key.writeCSV(writer, value));
    }
    
    printTime("Finished writing results", TimeUnit.MILLISECONDS);
  }
  
  private static void printTime(String message, TimeUnit unit) {
    long now = System.currentTimeMillis();
    var unitSymbol = switch (unit) {
      case DAYS -> "d";
      case HOURS -> "h";
      case MINUTES -> "min";
      case SECONDS -> "s";
      case MILLISECONDS -> "ms";
      default -> unit.toString();
    };
    System.out.println(message + ": " + unit.convert(lastPrinted - now, TimeUnit.MILLISECONDS) + " " + unitSymbol);
    lastPrinted = now;
  }
  
  private static int[] loadRange(Properties properties, String key) {
    int[] range = Pattern.compile(",").splitAsStream(properties.getProperty(key))
          .mapToInt(Integer::parseInt)
          .limit(2)
          .sorted()
          .toArray();
    if (range.length == 2) {
      return range;
    } else {
      return new int[] {range[0], range[0]};
    }
  }
  
  private record Variables(int n, int m, int trials) implements Comparable<Variables> {

    private static final Comparator<Variables> COMPARATOR = Comparator.comparingInt(Variables::m)
        .thenComparingInt(Variables::n)
        .thenComparingInt(Variables::trials);

    private void writeCSV(BufferedWriter writer, double result) {
      writer.write(n);
      writer.write(',');
      writer.write(m);
      writer.write(',');
      writer.write(trials);
      writer.write(',');
      writer.write(Double.toString(result));
      writer.newLine();
    }
    
    @Override
    public int compareTo(Variables other) {
      return COMPARATOR.compare(this, other);
    }
  }

  private static double execute(Variables variables) {
    List<Integer> originalMultiset = IntStream.range(0, variables.n() * variables.m())
        .map(i -> i % variables.n() + 1)
        .boxed()
        .toList();

    return Stream.generate(() -> new LinkedList<>(originalMultiset))
        .limit(variables.trials())
        .parallel()
        .peek(list -> Collections.shuffle(list, new Random()))
        .mapToInt(list -> {
          for (int i = 1; ; i++) {
            int index = list.indexOf(i);
            if (index == -1) {
              return i - 1;
            }
            list.subList(0, index + 1).clear();
          }
        })
        .average()
        .orElseThrow();
  }
}
