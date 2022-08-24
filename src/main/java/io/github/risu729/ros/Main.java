import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
// import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Main {

  private static final Path SETTINGS = Path.of("src", "main", "resources", "settings.properties");
  
  public static void main(String[] args) throws IOException {
    
    // load settings
    var properties = new Properties();
    properties.load(Files.newInputStream(SETTINGS));
    final int maxNumber = Integer.parseInt(properties.getProperty("maxNumber"));
    final int duplication = Integer.parseInt(properties.getProperty("duplication"));
    final int trialTimes = Integer.parseInt(properties.getProperty("trialTimes"));

    // generate randomly ordered sequence
    // use RandomGenerator.of("L128X1024MixRandom")

    List<Integer> baseList = IntStream.range(0, maxNumber * duplication).map(i -> i % maxNumber).map(i -> ++i).boxed().toList();
    Supplier<List<Integer>> rosGenerator = () -> {
      List<Integer> list = new LinkedList<>(baseList);
      Collections.shuffle(list, new Random());
      return list;
    };

    ToIntFunction<List<Integer>> getResult = l -> {
      for (int i = 1; ; i++) {
        int index = l.indexOf(i);
        if (index == -1) {
          return i - 1;
        }
        l.subList(0, index + 1).clear();
      }
    };

    double average = Stream.generate(rosGenerator).limit(trialTimes).mapToInt(getResult).average().orElseThrow();
    System.out.println(average);
  }
}