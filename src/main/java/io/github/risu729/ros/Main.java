import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;
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
  private static final Path RESULT_PATH = Path.of("results").resolve(
      OffsetDateTime.now(ZoneOffset.UTC).toString() + ".csv");
  
  public static void main(String[] args) throws IOException {
    
    // load settings
    Function<String, List<Integer>> propertiesConverter = key -> {
      var properties = new Properties();
      try (InputStream input = Files.newInputStream(SETTINGS_PATH)) {
        properties.load(input);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
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
      for (int duplication = duplicationRange.get(0); duplication <= duplicationRange.get(1); duplication++) {
        List<Integer> trialTimesRange = propertiesConverter.apply("trialTimes");
        for (int trialTimes = trialTimesRange.get(0); trialTimes <= trialTimesRange.get(1); trialTimes++) {
          conditions.add(new Settings(maxNumber, duplication, trialTimes));
        }
      }
    }

    Map<Settings, Double> result = conditions.stream()
        .collect(Collectors.toMap(UnaryOperator.identity(), Main::execute, (e1, e2) -> e1, TreeMap::new));
    
    Files.createDirectories(RESULT_PATH.getParent());
    Files.createFile(RESULT_PATH);
    try (BufferedWriter writer = Files.newBufferedWriter(RESULT_PATH)) {
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

  private record Settings(int maxNumber, int duplication, int trialTimes) implements Comparable{
    
    private static final Comparator COMPARATOR = Comparator.comparingInt(Settings::maxNumber)
        .thenComparingInt(Settings::duplication)
        .thenComparingInt(Settings::trialTimes);

    private String toCSV(double result) {
      return Stream.concat(IntStream.of(maxNumber, duplication, trialTimes).mapToObj(Integer::toString),
              Stream.of(Double.toString(result)))
          .collect(Collectors.joining(","));
    }
    
    @Override
    public int compareTo(Object other) {
      return COMPARATOR.compare(this, other);
    }
  }
}
