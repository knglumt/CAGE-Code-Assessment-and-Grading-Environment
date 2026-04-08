package org.assessment.codesplitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class CodeSplitter {

    private static ArrayList<Integer> findCandidates(
            ArrayList<Integer> possibilities,
            ArrayList<Integer> splits,
            HashMap<Integer, Integer> map) {

        ArrayList<Integer> candidates = new ArrayList<>();

        int startIndex = 0;

        if (!splits.isEmpty()) {
            Integer lastSplit = splits.get(splits.size() - 1);
            Integer mappedIndex = map.get(lastSplit);

            if (mappedIndex != null) {
                startIndex = mappedIndex + 1;
            }
        }

        for (int i = startIndex; i < possibilities.size(); i++) {
            candidates.add(possibilities.get(i));
        }

        return candidates;
    }

    private static Pair<ArrayList<Integer>, Double> backtrack(
            ArrayList<Integer> splits,
            PointCalculator calculator,
            ArrayList<Integer> possibilities,
            ArrayList<String> codeLines,
            HashMap<Integer, Integer> map) throws CloneNotSupportedException {

        ArrayList<Integer> candidates = findCandidates(possibilities, splits, map);

        if (candidates.isEmpty() || splits.size() >= calculator.refCodeSize()) {
            return new Pair<>(
                    new ArrayList<>(splits),
                    calculator.calculate(splits, codeLines)
            );
        }

        Pair<ArrayList<Integer>, Double> best =
                new Pair<>(null, Double.NEGATIVE_INFINITY);

        for (Integer candidate : candidates) {

            splits.add(candidate);

            Pair<ArrayList<Integer>, Double> result =
                    backtrack(splits, calculator, possibilities, codeLines, map);

            if (result.getValue() > best.getValue()) {
                best = new Pair<>(new ArrayList<>(result.getKey()), result.getValue());
            }

            splits.remove(splits.size() - 1);
        }

        return best;
    }

    public static ArrayList<ArrayList<Integer>> calculateBestSplits(
            String folderName,
            org.assessment.codesplitter.PointCalculator calculator)
            throws FileNotFoundException, CloneNotSupportedException, org.assessment.codesplitter.BracesNotMatchException {

        File folder = new File(folderName);
        File[] listOfFiles = folder.listFiles();

        ArrayList<ArrayList<Integer>> allResults = new ArrayList<>();

        for (File file : Objects.requireNonNull(listOfFiles)) {

            if (!file.isFile() || file.getName().equals("RefCode.java")) {
                continue;
            }

            ArrayList<String> codeLines = readFile(file);

            ArrayList<Integer> possibilities = Splitter.split(codeLines);

            // 🔥 FIX 2: ensure sorted (important for backtracking correctness)
            Collections.sort(possibilities);

            HashMap<Integer, Integer> map = buildIndexMap(possibilities);

            Pair<ArrayList<Integer>, Double> bestPair =
                    backtrack(new ArrayList<>(), calculator, possibilities, codeLines, map);

            allResults.add(bestPair.getKey());
        }

        return allResults;
    }

    public static ArrayList<ArrayList<Integer>> calculateBestSplitsforFile(
            File file,
            PointCalculator calculator)
            throws FileNotFoundException, CloneNotSupportedException, org.assessment.codesplitter.BracesNotMatchException {

        ArrayList<String> codeLines = readFile(file);

        ArrayList<Integer> possibilities = Splitter.split(codeLines);

        // 🔥 FIX 2: ensure sorted
        Collections.sort(possibilities);

        HashMap<Integer, Integer> map = buildIndexMap(possibilities);

        Pair<ArrayList<Integer>, Double> bestPair =
                backtrack(new ArrayList<>(), calculator, possibilities, codeLines, map);

        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        result.add(bestPair.getKey());

        return result;
    }

    // ---------------- HELPERS ----------------

    private static ArrayList<String> readFile(File file) throws FileNotFoundException {
        ArrayList<String> lines = new ArrayList<>();
        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            lines.add(scanner.nextLine());
        }

        scanner.close();
        return lines;
    }

    private static HashMap<Integer, Integer> buildIndexMap(ArrayList<Integer> possibilities) {
        HashMap<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < possibilities.size(); i++) {
            map.put(possibilities.get(i), i);
        }

        return map;
    }
}