import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import javax.swing.*;

/**
 * This class is designed to send emails to a list of students based on data provided through various files.
 */
public class EmailSender {

        private final String textFilePath;
        private final String folderPath;
        private final String emailConfigFile;
        private Session session;
        private String smtpUser;
        private int mailCount;
        private final List<String[]> emailedStudents = new ArrayList<>();

        public EmailSender(String textFilePath, String folderPath, String emailConfigFile) {
                this.textFilePath = textFilePath;
                this.folderPath = folderPath;
                this.emailConfigFile = emailConfigFile;
        }

        public void run(String subject) {
                List<String[]> studentData = readStudentData();
                setEmailConfig();
                mailCount = 0;
                emailedStudents.clear();

                for (String[] student : studentData) {
                        if (student.length < 2) {
                                System.err.println("Skipping malformed student entry.");
                                continue;
                        }
                        String studentId = student[0].trim();
                        String email = student[1].trim();

                        Map<String, StringBuilder> allFiles = scanAndConsolidate(studentId);

                        Map<String, StringBuilder> studentFiles = new HashMap<>(allFiles);
                        Map<String, StringBuilder> refCodeFiles = new HashMap<>(allFiles);
                        studentFiles.keySet().removeIf(key -> key.toLowerCase().contains("refcode"));
                        refCodeFiles.keySet().removeIf(key -> !key.toLowerCase().contains("refcode"));

                        sendCombinedEmail(subject, studentId, email, studentFiles, refCodeFiles);

                        try {
                                Thread.sleep(1500);
                        } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                        }
                }

                // Build scrollable summary dialog listing all emailed students
                StringBuilder summary = new StringBuilder();
                summary.append(String.format("%-20s %s%n", "Student ID", "Email"));
                summary.append("-".repeat(50)).append("\n");
                for (String[] s : emailedStudents) {
                        summary.append(String.format("%-20s %s%n", s[0], s[1]));
                }

                JTextArea textArea = new JTextArea(summary.toString());
                textArea.setEditable(false);
                textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
                textArea.setCaretPosition(0);

                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new java.awt.Dimension(500, 300));

                JOptionPane.showMessageDialog(
                        null,
                        scrollPane,
                        mailCount + " emails have been sent!",
                        JOptionPane.INFORMATION_MESSAGE
                );
        }

        // FIX: skip blank and malformed lines
        private List<String[]> readStudentData() {
                List<String[]> studentData = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(textFilePath))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                                if (line.isBlank()) continue;
                                String[] parts = line.split("\\s*[;,]\\s*");
                                if (parts.length < 2) {
                                        System.err.println("Skipping malformed line: " + line);
                                        continue;
                                }
                                studentData.add(parts);
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                }
                return studentData;
        }

        private Map<String, StringBuilder> scanAndConsolidate(String studentId) {
                Map<String, StringBuilder> consolidatedFiles = new HashMap<>();
                scanFolderRecursively(new File(folderPath), studentId, consolidatedFiles);
                return consolidatedFiles;
        }

        private void scanFolderRecursively(File folder, String studentId, Map<String, StringBuilder> consolidatedFiles) {
                File[] files = folder.listFiles();
                if (files == null) return;

                for (File file : files) {
                        if (file.isDirectory()) {
                                scanFolderRecursively(file, studentId, consolidatedFiles);
                        } else if (file.getName().toLowerCase().contains(studentId.toLowerCase()) ||
                                file.getName().toLowerCase().contains("refcode")) {

                                consolidatedFiles.computeIfAbsent(file.getName(), k -> new StringBuilder())
                                        .append(file.getParentFile().getName()).append("\n")
                                        .append(readFileContent(file.getPath())).append("\n\n\n");
                        }
                }
        }

        private void sendCombinedEmail(String subject, String studentId, String email,
                                       Map<String, StringBuilder> studentFiles,
                                       Map<String, StringBuilder> refFiles) {

                if (studentFiles.isEmpty() && refFiles.isEmpty()) {
                        System.err.println("No files found for student: " + studentId);
                        return;
                }

                try {
                        Message message = new MimeMessage(session);
                        message.setFrom(new InternetAddress(smtpUser));
                        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                        message.setSubject(subject);

                        StringBuilder body = new StringBuilder(studentId).append(":\n\n");

                        if (!studentFiles.isEmpty()) {
                                body.append("***STUDENT CODES***\n\n");
                                studentFiles.forEach((fileName, content) ->
                                        body.append(fileName).append("\n").append(content).append("\n\n"));
                        }

                        if (!refFiles.isEmpty()) {
                                body.append("***REFERENCE CODES***\n\n");
                                refFiles.forEach((fileName, content) ->
                                        body.append(fileName).append("\n").append(content).append("\n\n"));
                        }

                        message.setContent(body.toString(), "text/plain; charset=UTF-8");
                        Transport.send(message);
                        mailCount++;
                        // FIX: record successfully emailed student
                        emailedStudents.add(new String[]{studentId, email});

                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        private void setEmailConfig() {
                String[] emailConfig = readEmailConfig();

                Properties properties = new Properties();
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.starttls.enable", "true");
                properties.put("mail.smtp.ssl.protocols", "TLSv1.2");
                properties.put("mail.smtp.ssl.trust", emailConfig[0]);
                properties.put("mail.smtp.sendpartial", "true");
                properties.put("mail.smtp.host", emailConfig[0]);
                properties.put("mail.smtp.port", "587");

                session = Session.getInstance(properties, new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(emailConfig[1], emailConfig[2]);
                        }
                });
                smtpUser = emailConfig[1];
        }

        private String readFileContent(String filePath) {
                try {
                        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
                        return new String(bytes, StandardCharsets.UTF_8);
                } catch (IOException e) {
                        return "Error reading file content.";
                }
        }

        private String[] readEmailConfig() {
                try (BufferedReader reader = new BufferedReader(new FileReader(emailConfigFile))) {
                        String[] config = reader.readLine().split(",");
                        if (config.length < 3) throw new RuntimeException("Email config must have host, user, password.");
                        return config;
                } catch (IOException e) {
                        throw new RuntimeException("Invalid email configuration file.");
                }
        }
}