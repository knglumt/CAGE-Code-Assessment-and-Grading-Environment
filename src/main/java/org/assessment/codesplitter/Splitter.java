package org.assessment.codesplitter;

import java.util.*;

public class Splitter {


    private static String removeCommentsForParsing(String line) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        char prevChar = '\0';

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"' && prevChar != '\\' && !inChar) {
                inString = !inString;
                result.append(c);
            }
            else if (c == '\'' && prevChar != '\\' && !inString) {
                inChar = !inChar;
                result.append(c);
            }
            else if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/' && !inString && !inChar) {
                break;
            }
            else if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '*' && !inString && !inChar) {
                int endIndex = line.indexOf("*/", i + 2);
                if (endIndex != -1) {
                    i = endIndex + 1;
                } else {
                    break;
                }
            }
            else {
                result.append(c);
            }

            prevChar = c;
        }

        return result.toString();
    }

    private static ArrayList<String> createCleanedLines(ArrayList<String> lines) {
        ArrayList<String> cleaned = new ArrayList<>();
        boolean inMultiLineComment = false;

        for (String line : lines) {
            if (inMultiLineComment) {
                int endIndex = line.indexOf("*/");
                if (endIndex != -1) {
                    inMultiLineComment = false;
                    cleaned.add(line.substring(endIndex + 2));
                } else {
                    cleaned.add("");
                }
            } else {
                int startIndex = line.indexOf("/*");
                if (startIndex != -1) {
                    int endIndex = line.indexOf("*/", startIndex + 2);
                    if (endIndex != -1) {
                        String before = line.substring(0, startIndex);
                        String after = line.substring(endIndex + 2);
                        cleaned.add(removeCommentsForParsing(before + after));
                    } else {
                        inMultiLineComment = true;
                        cleaned.add(removeCommentsForParsing(line.substring(0, startIndex)));
                    }
                } else {
                    cleaned.add(removeCommentsForParsing(line));
                }
            }
        }

        return cleaned;
    }


    private static ArrayList<Pair<Command, Integer>> createCommands(ArrayList<String> cleanedLines)
            throws BracesNotMatchException {

        ArrayList<Pair<Command, Integer>> commands = new ArrayList<>();
        int i = 1;

        if (cleanedLines.get(i).trim().equals("{")) {
            i++;
        }

        boolean inComment = false;
        int braceStack = 0;

        while (i < cleanedLines.size() - 1) {

            String line = cleanedLines.get(i).trim();

            if (line.contains("/*")) {
                inComment = true;
            }
            else if (inComment) {
                if (line.contains("*/")) {
                    inComment = false;
                }
            }
            else if (line.isEmpty() || line.startsWith("//")) {
                // skip
            }
            else {

                if (braceStack > 0) {
                    if (line.contains("{")) braceStack++;

                    if (line.contains("}")) {
                        braceStack--;
                        if (braceStack == 0) {
                            commands.add(new Pair<>(Command.CLOSE, i));
                        }
                    }
                }
                else {
                    if (isControlStatement(line)) {
                        commands.add(new Pair<>(Command.OPEN, i));

                        String next = line;
                        while (!next.contains("{")) {
                            if (next.contains(";")) {
                                commands.add(new Pair<>(Command.CLOSE, i));
                                braceStack--;
                                break;
                            }
                            i++;
                            next = cleanedLines.get(i).trim();
                        }
                        braceStack++;
                    }
                    else if (line.contains("else")) {
                        int tempStack = braceStack;
                        String next = line;

                        while (!next.contains("{")) {
                            if (next.contains(";")) break;
                            i++;
                            next = cleanedLines.get(i).trim();
                        }

                        braceStack++;

                        while (tempStack != braceStack) {
                            i++;
                            next = cleanedLines.get(i).trim();
                            if (next.contains("{")) braceStack++;
                            if (next.contains("}")) braceStack--;
                        }
                    }

                    if (line.contains(";") && braceStack == 0) {
                        commands.add(new Pair<>(Command.STATEMENT, i));
                    }

                    if (line.contains("}")) {
                        if (braceStack <= 0) {
                            throw new BracesNotMatchException();
                        } else {
                            commands.add(new Pair<>(Command.CLOSE, i));
                            braceStack--;
                        }
                    }
                }
            }

            i++;
        }

        return commands;
    }

    private static boolean isControlStatement(String line) {
        return (line.startsWith("if") && !line.startsWith("else")) ||
                line.startsWith("while") ||
                line.startsWith("for");
    }

    // ---------------- BRACE CHECK ----------------

    private static boolean bracesCheck(ArrayList<Pair<Command, Integer>> commands) {
        int open = 0, close = 0;
        for (Pair<Command, Integer> command : commands) {
            if (command.getKey() == Command.OPEN) open++;
            if (command.getKey() == Command.CLOSE) close++;
        }
        return open == close;
    }

    // ---------------- NEW SPLIT LOGIC ----------------

    public static ArrayList<Integer> split(ArrayList<String> lines)
            throws BracesNotMatchException {

        ArrayList<String> cleanedLines = createCleanedLines(lines);

        ArrayList<Pair<Command, Integer>> commands = createCommands(cleanedLines);

        if (!bracesCheck(commands)) {
            throw new BracesNotMatchException(lines);
        }

        ArrayList<Integer> splitLines = new ArrayList<>();
        int stack = 0;

        for (Pair<Command, Integer> command : commands) {

            if (command.getKey() == Command.OPEN) {
                if (stack == 0) {
                    splitLines.add(command.getValue());
                }
                stack++;
            }
            else if (command.getKey() == Command.STATEMENT) {
                if (stack == 0) {
                    splitLines.add(command.getValue());
                }
            }
            else {
                stack--;
            }
        }

        Collections.sort(splitLines);

        if (splitLines.isEmpty()) {
            splitLines.add(cleanedLines.size() - 1);
        }

        return splitLines;
    }
}