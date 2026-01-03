# CAGE: Code Assessment and Grading Environment

## Overview

The **CAGE** is a Java-based application designed to assist instructors or reviewers in evaluating programming code efficiently. It combines file navigation, segment-based feedback, grading, and export capabilities within a user-friendly interface.

## Key Features

### 1. User-Friendly Interface
- Intuitive GUI with editable code view and line numbers.
- Split-pane layout with feedback tree on the right.
- Top and bottom control panels for quick access to operations.

### 2. File Operations
- **Open**: Load `.java`, `.txt`, `.cpp`, `.py`, and other supported code files.
- **Save and Next**: Save changes and automatically open the next file in the directory.
- **Previous**: Navigate to the previous file.

### 3. Code Grading & Comment Insertion
- Double-click to insert structured `/** ASSESSMENT */` blocks with `@grade` and `@feedback`.
- Automatic matching against a reference file for segment comparison.
- Grade and feedback suggestions presented in a tree view.

### 4. Read-Only & Editable Modes
- Toggle between segmentation-only (read-only) and grading mode (editable).
- Prevent accidental comment insertion inside control structures.

### 5. Undo Support
- Use `Ctrl+Z` to revert the last changes via a custom stack-based history.

### 6. Export Assessment to CSV
- One-click export of all assessment data to a timestamped `.csv` file.

### 7. Email Integration
- Automatically email individual results to students based on configuration and file mappings.

### 8. Assessment Feedback via Tree View
- View, select, and reuse predefined feedback items from a categorized tree.
- Tooltip preview of feedback-related code snippets.
- Prevent double-counting of reused feedback with a dedicated counter.

### 9. Line Number Enhancements
- Line numbers visually indicate segment boundaries.
- Red highlighting for mismatch in expected vs. actual segments.

### 10. Violation Detection
- Automatically warns about forbidden use of:
  - External data structures
  - Array or collection modifications

## Getting Started

### Requirements
- Java 8 or later

### Run Instructions
```
javac CodeAssessment.java
java CodeAssessment
```
Or:
```
java -jar CodeAssessment.jar
```

## Usage Tips

- Use **Open** to select the file to assess.
- Double-click on a code line to insert or update an assessment block.
- Select feedback from the tree and double-click to apply.
- Use **Next** or **Previous** to navigate through student files.
- Click **Export CSV** to generate a report.
- Click **Send Mails** to email feedback automatically.

## Contributing

Pull requests, issues, and feature suggestions are welcome. Feel free to fork the repo and make your changes.

## License

This tool is licensed under the [MIT License](LICENSE). Use, modify, and distribute freely.

## Citation

If you use **CAGE** in any academic study, please cite the following article:

[DOI: 10.1109/UBMK63289.2024.10773535](https://doi.org/10.1109/UBMK63289.2024.10773535)
