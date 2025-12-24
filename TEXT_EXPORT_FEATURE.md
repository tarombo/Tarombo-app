# Text Export Feature for Diagram

This feature adds a "Text" format option when sharing or exporting family tree diagrams.

## Implementation Summary

The feature has been implemented but encountered Java version compatibility issues (the project uses Java 8 but some code used Java 11+ features).

## Required Changes

### 1. Add "Text" option to menu dialogs

In the Share and Export dialogs, add `getText(R.string.text)` to the format arrays.

### 2. Add string resource

Add to `app/src/main/res/values/strings.xml`:
```xml
<string name="text_exported_ok">Text exported.</string>
```

The "text" string already exists in the file.

### 3. Implement text generation

Create two helper methods:
- `shareDiagramAsText()` - Similar to shareDiagramAsPDF but generates .txt file
- `generateFamilyTreeText()` - Recursively builds text representation of the tree

### 4. Add export handler

In `onActivityResult()`, add case for requestCode 906 to handle text export.

## Key Technical Notes

1. **Java 8 Compatibility**: Avoid `String.repeat()` - use loops instead
2. **Context Access**: Use `getContext().getString()` for string resources in helper methods  
3. **Person Names**: Use `U.epiteto(person)` to get formatted names
4. **Dates**: Use `U.twoDates(person, false)` to get birth/death dates
5. **File Extension**: Use `.txt` for text files
6. **MIME Type**: Use `"text/plain"` for sharing

## Text Format

The exported text uses a tree structure with:
- Tree title as header
- Current person as root
- Indented children with `├─` prefix
- Spouse indicator with `⚭` symbol
- Parents section at the end
- Dates in parentheses after names

## Status

The code was partially implemented but needs to be completed due to Java compatibility issues. The structure is ready for completion.
