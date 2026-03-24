package org.assessment.codesplitter;

import org.assessment.codesplitter.BracesNotMatchException;
import org.assessment.codesplitter.Command;
import org.assessment.codesplitter.Pair;

import java.util.*;

/**
 * Splitting logic ported from CS401 {@code SplitterCopy}.
 *
 * <p>Improvements over the original CAGE {@code Splitter}:</p>
 * <ul>
 *   <li>Comment stripping — {@code //}, inline {@code /* ... *\/}, and
 *       multi-line block comments are removed before parsing, so braces
 *       or keywords inside comments never produce phantom commands.</li>
 *   <li>Stack-based command parser ({@code createCommandsNew}) — uses a
 *       {@code braceStack} counter instead of naive per-line brace counting,
 *       so nested blocks are correctly skipped.</li>
 *   <li>{@code else} / {@code else-if} blocks are consumed silently and merged
 *       with their preceding {@code if}, so an if/else chain produces exactly
 *       one split point.</li>
 *   <li>Stack-based {@code split()} — replaces the old recursive {@code solve()}
 *       with a simple linear pass; only top-level commands (stack depth 0) emit
 *       split points.</li>
 *   <li>Fallback — if no split points are found the last line index is returned
 *       so callers always get a non-empty list.</li>
 * </ul>
 *
 * <p>The public API ({@code split(ArrayList<String>)}) is unchanged, so
 * {@link CodeSplitter} and {@code LineNumberArea} need no modification.</p>
 */
public class Splitter {

    // -------------------------------------------------------------------------
    // Comment stripping
    // -------------------------------------------------------------------------

    /**
     * Strips {@code //} single-line and inline block comments from one line,
     * correctly ignoring {@code /} characters inside string or char literals.
     */
    private static String removeCommentsForParsing(String line) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean inChar   = false;
        char prevChar    = '\0';

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"' && prevChar != '\\' && !inChar) {
                inString = !inString;
                result.append(c);
            } else if (c == '\'' && prevChar != '\\' && !inString) {
                inChar = !inChar;
                result.append(c);
            } else if (c == '/' && i + 1 < line.length()
                    && line.charAt(i + 1) == '/' && !inString && !inChar) {
                break; // rest of line is a comment
            } else if (c == '/' && i + 1 < line.length()
                    && line.charAt(i + 1) == '*' && !inString && !inChar) {
                int endIndex = line.indexOf("*/", i + 2);
                if (endIndex != -1) {
                    i = endIndex + 1; // skip past */
                } else {
                    break; // comment continues onto the next line
                }
            } else {
                result.append(c);
            }

            prevChar = c;
        }

        return result.toString();
    }

    /**
     * Returns a comment-free copy of {@code lines}, preserving line count so
     * all indices remain valid against the original source.
     */
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
                    cleaned.add(""); // whole line inside block comment — preserve index
                }
            } else {
                int startIndex = line.indexOf("/*");
                if (startIndex != -1) {
                    int endIndex = line.indexOf("*/", startIndex + 2);
                    if (endIndex != -1) {
                        String before = line.substring(0, startIndex);
                        String after  = line.substring(endIndex + 2);
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

    // -------------------------------------------------------------------------
    // Brace validation
    // -------------------------------------------------------------------------

    private static boolean bracesCheck(ArrayList<Pair<Command, Integer>> commands) {
        int open = 0, close = 0;
        for (Pair<Command, Integer> command : commands) {
            if (command.getKey().equals(Command.OPEN))  open++;
            if (command.getKey().equals(Command.CLOSE)) close++;
        }
        return open == close;
    }

    // -------------------------------------------------------------------------
    // Command generation  (CS401 createCommandsNew)
    // -------------------------------------------------------------------------

    /**
     * Parses comment-free source lines and emits a flat list of structural
     * commands using a {@code braceStack} integer to track nesting depth.
     *
     * <p>At depth 0:</p>
     * <ul>
     *   <li>{@code if} / {@code while} / {@code for} → {@code OPEN}</li>
     *   <li>{@code else} / {@code else if} → consumed silently, no command emitted</li>
     *   <li>Statement ending in {@code ;} → {@code STATEMENT}</li>
     *   <li>{@code }} returning depth to 0 → {@code CLOSE}</li>
     * </ul>
     * <p>At depth &gt; 0: only brace counters are updated.</p>
     */
    private static ArrayList<Pair<Command, Integer>> createCommandsNew(
            ArrayList<String> cleanedLines) throws BracesNotMatchException {

        ArrayList<Pair<Command, Integer>> commands = new ArrayList<>();
        int i = 1; // line 0 is always the function signature

        // K&R style: opening brace alone on line 1
        if (cleanedLines.size() > 1 && cleanedLines.get(i).trim().equals("{")) {
            i++;
        }

        boolean bigComment = false;
        int braceStack = 0;

        while (i < cleanedLines.size() - 1) {
            String line = cleanedLines.get(i).trim();

            if (line.contains("/*")) {
                bigComment = true;

            } else if (line.isEmpty() || line.startsWith("//")) {
                // blank or comment-only line — skip

            } else if (bigComment) {
                if (line.contains("*/")) bigComment = false;

            } else {
                if (braceStack > 0) {
                    // ── Inside a nested block: only track braces ─────────
                    if (line.contains("{")) braceStack++;
                    if (line.contains("}")) {
                        braceStack--;
                        if (braceStack == 0) {
                            commands.add(new Pair<>(Command.CLOSE, i));
                        }
                    }

                } else {
                    // ── Base level ────────────────────────────────────────

                    if ((line.contains("if") && !line.contains("else"))
                            || line.contains("while")
                            || line.contains("for")) {

                        commands.add(new Pair<>(Command.OPEN, i));

                        String next = line;
                        while (!next.contains("{")) {
                            if (next.contains(";")) {
                                // Single-statement body (no braces)
                                commands.add(new Pair<>(Command.CLOSE, i));
                                braceStack--; // offset the ++ below → net 0
                                break;
                            }
                            i++;
                            next = cleanedLines.get(i).trim();
                        }
                        braceStack++;

                    } else if (line.contains("else")) {
                        // Consume the entire else / else-if block silently
                        int tempStack = braceStack;
                        String next = line;

                        while (!next.contains("{")) {
                            if (next.contains(";")) {
                                braceStack = tempStack;
                                break;
                            }
                            i++;
                            next = cleanedLines.get(i).trim();
                        }

                        braceStack++;
                        if (next.contains("}")) braceStack--;

                        while (tempStack != braceStack) {
                            i++;
                            next = cleanedLines.get(i).trim();
                            if (next.contains("{")) braceStack++;
                            if (next.contains("}")) braceStack--;
                        }
                    }

                    // Plain statement at base level
                    if (line.contains(";") && braceStack == 0) {
                        commands.add(new Pair<>(Command.STATEMENT, i));
                    }

                    // Closing brace at base level
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

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Splits a function body into structural split-point line indices.
     *
     * <p>The public signature is unchanged from the original CAGE
     * {@code Splitter}, so {@link CodeSplitter} and {@code LineNumberArea}
     * require no modification.</p>
     *
     * @param lines all lines of a single function (signature on line 0,
     *              closing {@code }} on the last line)
     * @return sorted list of zero-based split-point line indices
     * @throws BracesNotMatchException if braces do not balance
     */
    public static ArrayList<Integer> split(ArrayList<String> lines)
            throws BracesNotMatchException {

        ArrayList<String> cleanedLines = createCleanedLines(lines);
        ArrayList<Pair<Command, Integer>> commands = createCommandsNew(cleanedLines);

        if (!bracesCheck(commands)) {
            System.out.println("[Splitter] Braces do not match — aborting split.");
            throw new BracesNotMatchException(lines);
        }

        ArrayList<Integer> splitLines = new ArrayList<>();
        int stack = 0;

        for (Pair<Command, Integer> command : commands) {
            if (command.getKey().equals(Command.OPEN)) {
                if (stack == 0) splitLines.add(command.getValue());
                stack++;
            } else if (command.getKey().equals(Command.STATEMENT)) {
                if (stack == 0) splitLines.add(command.getValue());
            } else { // Command.CLOSE
                stack--;
            }
        }

        Collections.sort(splitLines);

        if (splitLines.isEmpty()) {
            splitLines.add(cleanedLines.size() - 1); // fallback: whole function = one segment
        }

        return splitLines;
    }
}