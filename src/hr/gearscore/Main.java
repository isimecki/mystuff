package hr.gearscore;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static int[] sortOrder;
    public static int[] statMaxValues = new int[1];
    public static int[] statMinValues = new int[1];

    public static void main(String[] args) {
        Map<String, List<Gear>> gear = new HashMap();
        CSVFormat csvFormat = CSVFormat.newFormat(';')
                .withRecordSeparator("\r\n")
                .withQuote('"')
                .withIgnoreEmptyLines(true);

        BiFunction<String, String, Gear> gearValueExtractor = (name, value) -> new Gear(name, value.split(","));
        CSVParser records = null;
        try {
            records = CSVParser.parse(new File("resource/gear.csv"), Charset.defaultCharset(), csvFormat);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (CSVRecord record : records) {
            if (record.get(0).toLowerCase().contains("maxneeded")) {
                if (statMaxValues == null) {
                    statMaxValues = new int[0];
                }
                int statIdx = getIntColumnValueAt(record, 1);
                int statMaxValue = getIntColumnValueAt(record, 2);
                int[] tempStatMaxValues = new int[statMaxValues.length > (statIdx + 1) ? statMaxValues.length : (statIdx + 1)];
                for (int i = 0; i < statMaxValues.length; i++) {
                    tempStatMaxValues[i] = statMaxValues[i];
                }
                tempStatMaxValues[statIdx] = statMaxValue;
                statMaxValues = tempStatMaxValues;
                continue;
            }
            if (record.get(0).toLowerCase().contains("minneeded")) {
                if (statMinValues == null) {
                    statMinValues = new int[0];
                }
                int statIdx = getIntColumnValueAt(record, 1);
                int statMinValue = getIntColumnValueAt(record, 2);
                int[] tempStatMinValues = new int[statMinValues.length > (statIdx + 1) ? statMinValues.length : (statIdx + 1)];
                for (int i = 0; i < statMinValues.length; i++) {
                    tempStatMinValues[i] = statMinValues[i];
                }
                tempStatMinValues[statIdx] = statMinValue;
                statMinValues = tempStatMinValues;
                continue;
            }
            if (record.get(0).toLowerCase().contains("sort")) {
                sortOrder = Stream.of(record.get(1).split(",")).mapToInt(Integer::valueOf).toArray();
                continue;
            }
            Optional<List<Gear>> gearInSlots = Optional.ofNullable(gear.get(record.get(0)));
            if (gearInSlots.isPresent()) {
                gear.get(record.get(0)).add(gearValueExtractor.apply(record.get(1), record.get(2)));
            } else {
                gear.put(record.get(0), new ArrayList());
                gear.get(record.get(0)).add(gearValueExtractor.apply(record.get(1), record.get(2)));
            }
        }

        List<Gear> combinations = new ArrayList();
        fillGearCombinations(combinations, new ArrayList(), gear.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        combinations = combinations.stream().filter(o -> {
            int statIdx = 0;
            for (int stat : statMaxValues) {
                if (stat < o.stats[statIdx]) {
                    return false;
                }
                statIdx++;
            }
            return true;
        }).filter(o -> {
            int statIdx = 0;
            for (int stat : statMinValues) {
                if (stat > o.stats[statIdx]) {
                    return false;
                }
                statIdx++;
            }
            return true;
        }).collect(Collectors.toList());
        Collections.sort(combinations);
        combinations.stream().forEach(System.out::println);
    }

    private static Integer getIntColumnValueAt(CSVRecord record, int i) {
        return Integer.valueOf(record.get(i));
    }


    private static void fillGearCombinations(List<Gear> combinations, List<Gear> currentCombination, Map<String, List<Gear>> restOfItems) {
        List<Gear> gears = restOfItems.keySet().stream().findFirst().map(restOfItems::remove).get();
        if (restOfItems.isEmpty()) {
            for (Gear gear : gears) {
                List<Gear> newList = currentCombination.stream().collect(Collectors.toList());
                newList.add(gear);
                combinations.add(new Gear(null, newList));
            }
        } else {
            for (Gear gear : gears) {
                List<Gear> newCombination = currentCombination.stream().collect(Collectors.toList());
                newCombination.add(gear);
                fillGearCombinations(combinations, newCombination, restOfItems.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            }
        }
    }

    public static class Gear implements Comparable<Gear> {
        String name;
        int[] stats;
        List<Gear> items;

        public Gear(int[] stats) {
            this.stats = stats;
        }

        public Gear(String name, String... stat) {
            this.name = name;
            stats = Stream.of(stat).mapToInt(Integer::valueOf).toArray();
        }

        public Gear(String name, List<Gear> items) {
            this.name = name;
            this.items = items;
            this.stats = new int[items.stream().findFirst().get().stats.length];
            computeStats();
        }


        public void computeStats() {
            this.items.stream().forEach(gear -> {
                int idx = 0;
                for (int stat : gear.stats) {
                    this.stats[idx] += stat;
                    idx++;
                }
            });
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            Optional.ofNullable(name).map(s -> sb.append(s).append("-")).orElse(sb)
                    .append(Arrays.stream(stats).boxed().map(integer -> "" + integer).reduce((s, s2) -> s.concat("|" + s2)).get())
                    .append("\n")
                    .append(items.stream().map(gear -> gear.name).collect(Collectors.joining(",")));
            return sb.toString();
        }

        @Override
        public int compareTo(Gear o) {
            for (int sortIndex : sortOrder) {
                if (o.stats[sortIndex] - stats[sortIndex] != 0) {
                    return o.stats[sortIndex] - stats[sortIndex];
                }
            }
            return 0;
        }
    }
}
