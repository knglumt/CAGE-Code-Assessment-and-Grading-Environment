package org.assessment.codesplitter;

import java.util.ArrayList;

public class BracesNotMatchException extends Exception {

    private final ArrayList<String> lines;

    public BracesNotMatchException() {
        this.lines = new ArrayList<>();
    }


    public BracesNotMatchException(ArrayList<String> lines) {
        this.lines = lines;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        return "Braces do not match:\n" + sb;
    }
}