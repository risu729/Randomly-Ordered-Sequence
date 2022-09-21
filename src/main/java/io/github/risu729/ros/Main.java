import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.function.UnaryOperator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Main {

  private static final Path SETTINGS_PATH = Path.of("src", "main", "resources", "settings.properties");
  private static final Path RESULTS_PATH = Path.of("results").resolve(OffSetDateTime.now(ZoneOffSet.UTC).toString());
  
  public static void main(String[] args) throws IOException {
    
    // load settings
    Function<String, List<Integer>> propertiesConverter = key -> {
      var properties = new Properties();
      properties.load(Files.newInputStream(SETTINGS_PATH));
      List<Integer> result = Pattern.compile(",").splitAsStream(properties.getProperty(key))
          .mapToInt(Integer::parseInt)
          .limit(2)
          .sorted()
          .boxed()
          .collect(Collectors.toList());
      if (result.size() == 1) {
        result.add(result.get(0));
      }
      Collections.unmodifiableList(result);
      return result;
    };

    List<Settings> conditions = new LinkedList<>();
    List<Integer> maxNumberRange = propertiesConverter.apply("maxNumber");
    for (int maxNumber = maxNumberRange.get(0); maxNumber <= maxNumberRange.get(1); maxNumber++) {
      List<Integer> duplicationRange = propertiesConverter.apply("duplication");
      for (int duplication = duplication.get(0); duplication <= duplication.get(1); duplication++) {
        List<Integer> trialTimesRange = propertiesConverter.apply("trialTimesRange");
        for (int trialTimes = trialTimesRange.get(0); trialTimes <= trialTimesRange.get(1); trialTimes++) {
          conditions.add(new Settings(maxNumber, duplication, trialTimes));
        }
      }
    }

    Map<Settings, Double> result = conditions.stream()
        .collect(Collectors.toUnmodifiableMap(UnaryOperator.identity(), Main::execute));
    try (BufferedWriter writer = Files.newBufferedWriter(RESULTS_PATH)) {
      writer.write(String.join(",", "maxNumber", "duplication", "trialTimes", "result"));
      writer.newLine();
      for (var entry : result.entrySet()) {
        writer.write(entry.getKey().toCSV(entry.getValue()));
        writer.newLine();
      }
    }
  }

  private static double execute(Settings settings) {
    List<Integer> baseList = IntStream.range(0, settings.maxNumber() * settings.duplication())
        .map(i -> i % settings.maxNumber())
        .map(i -> ++i)
        .boxed()
        .toList();

    return Stream.generate(() -> {
      List<Integer> list = new LinkedList<>(baseList);
      Collections.shuffle(list, new Random());
      return list;
    })
    .limit(settings.trialTimes())
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

  private record Settings(int maxNumber, int duplication, int trialTimes) {

    private static final CSVHeader = ;

    private String toCSV(double result) {
      return Stream.concat(IntStream.of(maxNumber, duplication, trialTimes).map(Integer::toString),
              Stream.of(Double.toString(result)))
          .collect(Collectors.joining(","));
    }
  }
}