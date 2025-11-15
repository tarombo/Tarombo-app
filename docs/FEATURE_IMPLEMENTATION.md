# Tarombo Features Implementation Guide

**Created:** 2025-10-31  
**Last Updated:** 2025-11-16

## Overview

This document describes all the features implemented in Tarombo for Batak Toba kinship system support, including user interface enhancements, relationship detection capabilities, and visualization tools.

## Feature Summary

### 🎯 Core Features Implemented
- **Comprehensive Batak Toba kinship detection** with authentic cultural terminology
- **Interactive relationship diagrams** showing genealogical connections
- **Performance-optimized algorithms** preventing app crashes in complex family trees
- **Sibling inheritance logic** ensuring cultural consistency
- **Gender-sensitive relationship terms** maintaining cultural accuracy

---

## 1. Batak Toba Kinship System Selection

### How to Enable
1. **Navigate to Settings**: Open app settings menu
2. **Select Kinship Terms**: Choose "Kinship terms" option
3. **Choose Batak Toba**: Select "Batak Toba" from available systems
4. **Automatic Activation**: System immediately applies to all relationship calculations

### What Changes
- All relationship displays use authentic Batak Toba terminology
- Cultural context information appears with each relationship
- "Show" button becomes available for relationship diagrams
- Dalihan Na Tolu classification appears in relationship descriptions

---

## 2. Relationship Detection Engine

### Supported Relationship Types

#### Direct Relationships
- **Spouse relationships**: Husband/Wife with cultural context
- **Parent-child**: Direct generational connections with proper terms
- **Siblings**: Brother/sister relationships with age consideration

#### Extended Family (3-Person Paths)
- **Parent's siblings**: Amanguda (Father's brother), Tulang (Mother's brother)
- **Sibling's spouses**: Eda (Brother's wife), Lae (Sister's husband)
- **Child's spouses**: Parumaen (Son's wife), Hela (Daughter's husband)

#### Complex Relationships (4-Person Paths)
- **Parent's sibling's spouses**: Nantulang (Mother's brother's wife)
- **Spouse's family**: In-law relationships with cultural classification
- **Co-parent relationships**: Bao (co-parent-in-law)

#### Advanced Patterns (5+ Person Paths)
- **Sibling inheritance**: Brothers/sisters share same relationship terms
- **Extended cultural connections**: Multi-generational relationships
- **Complex affinal patterns**: Marriage-based connections through multiple steps

### Technical Implementation
- **Breadth-First Search**: Finds shortest connection paths between any two people
- **Multi-pattern analysis**: Applies cultural logic to discovered paths
- **Performance optimization**: 100-iteration limit prevents app freezing
- **Memory efficiency**: Smart caching and cleanup for large family trees

---

## 3. Interactive Relationship Diagrams

### Overview

A comprehensive visualization feature specifically designed for the Batak Toba kinship system that allows users to understand the genealogical basis of kinship relationships through interactive diagrams. This feature provides both educational value and practical understanding of complex cultural relationships.

### Activation Requirements
- **Batak Toba system selected**: Must be chosen in Settings → Kinship terms
- **Valid relationship detected**: System found a cultural relationship
- **Two-person comparison**: Viewing relationship between specific individuals

### When Available
- **Trigger**: Only appears when "Batak Toba" is selected as the kinship term in Settings
- **Location**: Relationship result dialog when viewing person-to-person relationships
- **UI Element**: "Show" button appears to the left of the "OK" button

### User Experience Flow

#### Step 1: Enable Batak Toba System
1. **Set Kinship System**: User selects "Batak Toba" in Settings → Kinship terms
2. **System Activation**: All relationship calculations now use Batak terminology

#### Step 2: View Relationship
1. Select a person in the family tree
2. Tap on another person to see relationship
3. **Relationship dialog appears** with Batak Toba kinship term (e.g., "Tulang", "Pariban")

#### Step 3: Access Diagram
1. **"Show" button appears**: To the left of "OK" button
2. **Tap "Show"**: Opens detailed kinship diagram
3. **Visual explanation**: Shows genealogical path and cultural context

### Technical Implementation

#### Code Architecture

**Modified Files:**
- **`Anagrafe.java`**: Added "Show" button logic and navigation to diagram
- **`BatakKinshipDiagramActivity.java`**: New activity for diagram display and rendering
- **`activity_batak_kinship_diagram.xml`**: Layout for diagram screen
- **`AndroidManifest.xml`**: Activity registration

**Key Components:**

```java
// Show button logic in Anagrafe.java
if ("batak_toba".equals(Global.settings.kinshipTerms)) {
    builder.setNeutralButton("Show", (dialog, which) -> {
        showBatakKinshipDiagram(parente, perno, result);
        dialog.dismiss();
    });
}
```

```java
// Custom diagram view with canvas drawing
public static class BatakDiagramView extends View {
    // Draws genealogical relationships visually
    // Shows common ancestors, generation paths, and kinship categories
    
    @Override
    protected void onDraw(Canvas canvas) {
        // Custom rendering of relationship diagram
        // Includes boxes, lines, labels, and cultural context
    }
}
```

### Diagram Features

#### Visual Elements

**Component Layout:**
```
┌─────────────────────────────────────┐
│           Title Bar                 │
│   "Relationship: A to B"            │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────┐            │
│  │  Common Ancestor    │ ← Yellow   │
│  │     (if any)        │            │
│  └─────────────────────┘            │
│           │                         │
│      ┌────┴────┐                    │
│      │         │                    │
│  ┌───▼───┐ ┌───▼───┐                │
│  │Person │ │Person │ ← Blue/Green   │
│  │   A   │ │   B   │                │
│  └───────┘ └───────┘                │
│      └─────────┘                    │
│           │                         │
│  ┌────────▼────────┐                │
│  │   Relationship  │ ← Orange       │
│  │     "Tulang"    │                │
│  └─────────────────┘                │
│           │                         │
│  ┌────────▼────────┐                │
│  │  Hula-hula      │ ← Cyan         │
│  │  (Wife-giving)  │                │
│  └─────────────────┘                │
│                                     │
├─────────────────────────────────────┤
│      Scrollable Explanation         │
│        Text Area                    │
│  • Cultural significance            │
│  • Social obligations               │
│  • Ceremonial roles                 │
└─────────────────────────────────────┘
```

**Visual Components:**
- **Common Ancestor**: Yellow box at the top showing genealogical connection point (for blood relations)
- **Person A & Person B**: Colored boxes (light blue and light green) showing the two related people
- **Generation Lines**: Lines with generation count showing path to common ancestor
- **Relationship Line**: Direct connection between the two people
- **Relationship Label**: Orange box displaying the Batak Toba kinship term
- **Dalihan Na Tolu Category**: Cyan box showing which category the relationship belongs to

#### Educational Content
- **Relationship Explanation**: Detailed text explaining the cultural significance
- **Dalihan Na Tolu Context**: Categorization into Hula-hula, Dongan Tubu, or Boru
- **Cultural Significance**: Explanation of social obligations and customs
- **Generation Analysis**: Breakdown of genealogical distance from common ancestor

### Visual Design Specifications

#### Color Coding System
- **Yellow (#FFEB3B)**: Common ancestors - genealogical connection points
- **Light Blue (#90CAF9)**: Person A - one person in the relationship
- **Light Green (#A5D6A7)**: Person B - the other person in the relationship
- **Orange (#FF9800)**: Relationship term display - the Batak kinship term
- **Cyan (#4DD0E1)**: Dalihan Na Tolu category - cultural classification
- **Gray (#BDBDBD)**: Box backgrounds, connecting lines, and borders

#### Typography
- **Title**: Bold, 18sp
- **Relationship Term**: Bold, 16sp
- **Category Labels**: Regular, 14sp
- **Explanation Text**: Regular, 14sp with comfortable line spacing

#### Layout Measurements
- **Box padding**: 16dp
- **Line thickness**: 2dp for relationships, 4dp for generation paths
- **Spacing**: 24dp between major elements
- **Margins**: 16dp from screen edges

### Cultural Intelligence Integration

#### Dalihan Na Tolu Categories

The diagram automatically categorizes relationships into the three pillars:

**1. Hula-hula (Wife Givers) - Superior Position**
- **Relationships**: Tulang, Pariban, Lae relationships
- **Cultural Explanation**: "Must be honored - wife-giving families hold highest respect"
- **Social Obligations**: Blessing givers, ceremonial authority, ultimate respect
- **Visual**: Cyan category box with explanatory text

**2. Dongan Tubu (Same Clan) - Equal Position**
- **Relationships**: Same clan cousins, siblings, paternal relatives
- **Cultural Explanation**: "Equal standing - mutual support and cooperation within clan"
- **Social Obligations**: Shared responsibilities, mutual respect, clan solidarity
- **Visual**: Cyan category box with clan context

**3. Boru (Wife Takers) - Service Position**
- **Relationships**: Namboru, Amangboru, Hela relationships
- **Cultural Explanation**: "Service and respect - those who marry clan women"
- **Social Obligations**: Provides service, shows deference, respectful assistance
- **Visual**: Cyan category box with service context

#### Relationship-Specific Explanations

**Tulang (Mother's Brother):**
```
Tulang is the central authority figure in Batak Toba society. As your 
mother's brother, he represents the Hula-hula (wife-giving) family. In 
traditional ceremonies, Tulang gives blessings and must be honored above 
all others. His authority is respected in all family decisions.
```

**Pariban (Cross-Cousin):**
```
Pariban is your mother's brother's daughter or father's sister's daughter. 
In traditional Batak culture, Pariban represents the ideal marriage partner 
as it strengthens ties between the Hula-hula and Boru families, reinforcing 
the Dalihan Na Tolu system.
```

**Dongan Sahala (Distant Relative):**
```
Dongan Sahala refers to distant clan relatives who share the same marga 
(clan name) but have no direct traceable genealogical connection. While 
distant, they are still part of the Dongan Tubu (same clan) network.
```

### Example Scenarios

#### Scenario 1: Tulang Relationship
**Path**: Person → Mother → Mother's Brother
- **Diagram Shows**: 
  - Yellow box: Mother (common path element)
  - Blue box: You (Person A)
  - Green box: Mother's Brother (Person B)
  - Orange label: "Tulang"
  - Cyan category: "Hula-hula (Wife-giving)"
- **Explanation**: "Tulang is your mother's brother, representing the wife-giving family. He holds the highest position of respect in Batak society and provides blessings in all ceremonies."

#### Scenario 2: Pariban Relationship  
**Path**: Person → Mother → Mother's Brother → Mother's Brother's Daughter
- **Diagram Shows**:
  - Yellow box: Mother's Brother (connection point)
  - Generation count: 2 generations from common ancestor
  - Orange label: "Pariban"
  - Cyan category: "Hula-hula (Marriageable Cousin)"
- **Explanation**: "Pariban is your mother's brother's daughter, traditionally considered an ideal marriage partner. This union strengthens family ties and reinforces the Dalihan Na Tolu system."

#### Scenario 3: Dongan Tubu Relationship
**Path**: Person → Father → Father's Brother → Father's Brother's Son
- **Diagram Shows**:
  - Yellow box: Father's Brother (common ancestor)
  - Same clan indication
  - Orange label: "Dongan Tubu"
  - Cyan category: "Same Clan (Equal Standing)"
- **Explanation**: "Your cousin through your father's brother represents Dongan Tubu - same clan relatives with equal standing. Mutual cooperation and support are expected."

### Cultural Benefits

#### Educational Value
- **Cultural Preservation**: Teaches authentic Batak kinship concepts to younger generations
- **Visual Learning**: Makes complex relationship systems easier to understand
- **Social Context**: Explains obligations and customs for each relationship type
- **Interactive Discovery**: Users learn by exploring their own family connections

#### Practical Applications
- **Family Ceremonies**: Helps understand proper protocols for weddings, funerals, and rituals
- **Marriage Planning**: Clarifies traditional partnership preferences and restrictions
- **Social Interaction**: Provides guidance for appropriate behavior toward relatives
- **Conflict Resolution**: Understanding relationship hierarchy aids in family mediation

### Performance Considerations

**Rendering Optimization:**
- Diagrams rendered on-demand, not pre-computed
- Canvas drawing optimized for smooth scrolling
- Text layout cached for repeated views
- Bitmap caching for static elements

**Memory Management:**
- Diagram data released when activity closes
- No persistent storage of diagram images
- Efficient string formatting for explanations

### Future Enhancements

#### Planned Improvements
- **Interactive Elements**: Clickable diagram components to explore connections
- **Multiple Ancestors**: Support for complex genealogical paths with multiple common ancestors
- **Audio Pronunciation**: Proper pronunciation guides for Batak terms
- **Extended Family**: Broader kinship network visualization beyond two people
- **Ceremonial Context**: Specific adat ceremony role explanations for each relationship
- **Sharing**: Export diagram as image for educational purposes
- **Print Support**: Generate printable family relationship charts

#### Advanced Features
- **Animation**: Animated path traversal showing how relationship was determined
- **Zoom/Pan**: Interactive diagram navigation for complex relationships
- **Comparison**: Side-by-side comparison of multiple relationships
- **Historical Context**: Integration with traditional Batak genealogical records

---

## 4. Performance Optimizations

### Problem Solved: ANR Prevention
- **Issue**: Complex family trees could cause "Application Not Responding" crashes
- **Solution**: Intelligent iteration limits and fast-path detection
- **Result**: Stable performance even with 1000+ person family trees

### Fast Relationship Detection
- **Sibling Detection**: Quick identification prevents unnecessary deep analysis
- **Path Caching**: Commonly accessed relationships cached for speed
- **Memory Management**: Efficient cleanup prevents memory leaks

### User Experience Improvements
- **Instant Response**: Most relationships detected within milliseconds
- **Graceful Fallback**: If complex analysis times out, shows "Non-relative"
- **Progress Indication**: Visual feedback during complex calculations

---

## 5. Cultural Accuracy Features

### Gender-Sensitive Detection
- **Automatic Gender Recognition**: System detects gender from genealogical data
- **Culturally Appropriate Terms**: Different terms for male/female relatives
- **Examples**:
  - Male parent's sibling = "Amanguda"
  - Female parent's sibling = "Inanguda" or "Nanguda"

### Dalihan Na Tolu Classification
Every relationship includes cultural context:
- **Hula-hula** (Wife-giving, superior): "Tulang (Mother's Brother - Hula-hula)"
- **Dongan Tubu** (Same clan, equal): "Amanguda (Father's Brother - Dongan Tubu)"
- **Boru** (Wife-receiving, inferior): "Amangboru (Father's Sister's Husband - Boru)"

### Sibling Consistency
- **Cultural Principle**: Siblings must have identical relationship terms
- **Implementation**: If one brother is "Amanguda", all brothers are "Amanguda"
- **Validation**: System verifies sibling status through genealogical data

---

## 6. User Interface Integration

### Settings Integration
- **Location**: Settings → Kinship terms
- **Options**: Traditional/English/Batak Toba
- **Persistence**: Choice saved across app sessions

### Relationship Display
- **In-app Integration**: Works with existing relationship viewing features
- **Dialog Enhancement**: Adds "Show" button for diagram access
- **Context Information**: Cultural classification included in descriptions

### Navigation Flow
```
Family Tree View
       ↓
Select Person A
       ↓
Tap Person B
       ↓
Relationship Dialog
       ↓
[Show] [OK] buttons
       ↓
Relationship Diagram
```

---

## 7. Testing and Validation

### Comprehensive Test Coverage
- **Family Scenarios**: Tested with multiple complex family structures
- **Cultural Accuracy**: Validated with Batak Toba cultural experts
- **Performance**: Stress-tested with large family networks
- **Cross-platform**: Verified on various Android devices and versions

### Real-world Validation
- **Authentic Terminology**: All terms verified with cultural sources
- **Social Context**: Proper Dalihan Na Tolu classification confirmed
- **Edge Cases**: Unusual family configurations handled gracefully

---

## 8. Future Enhancements

### Planned Improvements
- **Additional Cultural Systems**: Support for other Indonesian kinship systems
- **Ceremonial Integration**: Role assignment for traditional ceremonies
- **Educational Features**: Cultural learning modules and explanations
- **Performance**: Further optimization for very large family trees

### Community Features
- **Cultural Consultation**: Integration with community elders and experts
- **User Feedback**: System for reporting terminology corrections
- **Regional Variations**: Support for different Batak sub-groups

---

## Technical Requirements

### System Requirements
- **Android Version**: Minimum API 24 (Android 7.0)
- **Memory**: Optimized for devices with 2GB+ RAM
- **Storage**: Minimal additional storage requirements

### Dependencies
- **GEDCOM Library**: Family tree data structure support
- **Graph Algorithms**: Custom BFS implementation for relationship detection
- **UI Framework**: Android native components with custom diagram rendering

---

## Conclusion

The Batak Toba kinship system implementation in Tarombo represents a comprehensive solution for authentic cultural relationship detection and visualization. By combining advanced algorithms with genuine cultural knowledge, it provides both practical genealogy functionality and meaningful cultural preservation.

The interactive relationship diagrams serve as a powerful educational tool, making complex cultural concepts accessible through visual representation while maintaining strict cultural accuracy. This feature set transforms Tarombo from a simple genealogy app into a culturally-aware tool that respects and preserves traditional Batak Toba kinship knowledge while providing modern technological convenience.

---

**Author**: Arnold Siboro  
**Version**: 3.0  
**Last Updated**: 2025-11-16 (Merged with relationship diagram feature documentation)

---

## Document History

- **2025-10-31**: Initial creation with core features overview
- **2025-11-16**: Merged comprehensive relationship diagram feature documentation from batak_diagram_feature.md; expanded Section 3 with detailed visual design, cultural intelligence, example scenarios, and future enhancements