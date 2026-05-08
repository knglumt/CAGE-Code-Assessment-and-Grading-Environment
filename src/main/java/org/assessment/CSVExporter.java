import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The CSVExporter class provides methods for processing folders, extracting grades, and ensuring all students are listed,
 * including generating a CSV file with grades.
 */
public class CSVExporter {

    /**
     * Processes the specified root folder and student mail file, extracts student grades from files,
     * and ensures all listed students in the mail file are included, even if they have no grades.
     *
     * @param rootFolder The root folder containing subfolders of student files.
     * @param studentMailsFile The filename of the student mails within the root folder.
     * @return A map with subfolder names as keys and a map of student IDs to their grades as values.
     */
    static Map<String, Map<String, Map<String, Integer>>> processFolder(String rootFolder, String studentMailsFile) {
        Map<String, Map<String, Map<String, Integer>>> results = new HashMap<>();

        File folder = new File(rootFolder);
        File[] subfolders = folder.listFiles(File::isDirectory);

        if (subfolders != null) {
            for (File subfolder : subfolders) {
                String folderName = subfolder.getName();
                Map<String, Map<String, Integer>> studentData = new HashMap<>();

                for (File file : subfolder.listFiles(File::isFile)) {
                    String fileName = file.getName();
                    String studentId = getStudentId(fileName);

                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        int sum = 0;

                        Map<String, Integer> individualScores = new LinkedHashMap<>(); // Preserves insertion order

                        int section = 1;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("@grade")) {
                                int score = extractAndSumGrades(line);
                                sum += score;
                                individualScores.put("Score" + section, score);
                                section++;
                            }
                        }

                        individualScores.put("Total", sum);
                        studentData.put(studentId, individualScores);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                results.put(folderName, studentData);
            }
        }

        File mailsFile = new File(rootFolder, studentMailsFile);
        try (Scanner scanner = new Scanner(mailsFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                String[] parts = line.split("\\s*[;,]\\s*");
                String studentId = parts[0];

                if (!results.values().stream().anyMatch(m -> m.containsKey(studentId))) {
                    for (String folderName : results.keySet()) {
                        results.get(folderName).putIfAbsent(studentId, createEmptyGradeMap());
                    }
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Extracts the student ID from a given file name.
     *
     * @param fileName The name of the file.
     * @return The extracted student ID.
     */
    private static String getStudentId(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String nameWithoutExt = (dotIndex != -1) ? fileName.substring(0, dotIndex) : fileName;
        int underscoreIndex = nameWithoutExt.indexOf('_');
        return (underscoreIndex != -1) ? nameWithoutExt.substring(0, underscoreIndex) : nameWithoutExt;
    }

    /**
     * Creates an empty grade map for students with no grades.
     *
     * @return A map with a single entry "Total" set to 0.
     */
    private static Map<String, Integer> createEmptyGradeMap() {
        Map<String, Integer> emptyMap = new HashMap<>();
        emptyMap.put("Total", null);
        return emptyMap;
    }

    /**
     * Extracts and sums up grades from a line containing "@grade" comments.
     *
     * @param line The line containing grade information.
     * @return The sum of grades extracted from the line.
     */
    private static int extractAndSumGrades(String line) {
        String[] grades = line.replaceAll("[^0-9]+", " ").trim().split(" ");
        int sum = 0;
        for (String grade : grades) {
            if (!grade.isEmpty()) {
                if (line.contains("-"))
                    sum -= Integer.parseInt(grade);
                else
                    sum += Integer.parseInt(grade);
            }
        }
        return sum;
    }

    /**
     * Generates a CSV file based on the processed data, listing all students and their grades.
     *
     * @param results   The processed data containing subfolder names and student grades.
     * @param outputCsv The path to the output CSV file.
     */

    static void generateCsv(Map<String, Map<String, Map<String, Integer>>> results, String outputCsv) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputCsv))) {
            // Write CSV header
            writer.print("Student ID,");
            for (String folderName : results.keySet()) {
                writer.print(folderName + ",");
                writer.print(folderName + " Scores,");
            }
            writer.println("Total Grade");

            // Collect all student IDs
            Set<String> allStudentIds = results.values().stream()
                    .flatMap(folderData -> folderData.keySet().stream())
                    .collect(Collectors.toSet());

            // Write each student's data
            for (String studentId : allStudentIds) {
                writer.print(studentId + ",");

                int totalGrade = 0;
                boolean graded = false;

                for (Map.Entry<String, Map<String, Map<String, Integer>>> entry : results.entrySet()) {
                    Map<String, Integer> studentData = entry.getValue().getOrDefault(studentId, createEmptyGradeMap());

                    // Write folder grade (Total)
                    Integer grade = studentData.get("Total");
                    writer.print((grade != null ? grade : " ") + ",");
                    if (grade != null) {
                        totalGrade += grade;
                        graded = true;
                    }

                    // Write individual scores in order (Score1, Score2, ...)
                    for (int i = 1; i <= studentData.size() - 1; i++) { // Skip "Total"
                        String scoreKey = "Score" + i;
                        if (studentData.containsKey(scoreKey)) {
                            writer.print(studentData.get(scoreKey) + " ");
                        }
                    }
                    writer.print(","); // End of scores column
                }

                // Write total grade (if any grades exist)
                writer.println(graded ? totalGrade : " ");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
