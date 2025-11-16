# Batak Toba Kinship System: Complete Cultural Specification and Technical Implementation

**Created:** 2025-10-31  
**Last Updated:** 2025-11-16

---

## Table of Contents

### Part I: Cultural Specification
1. [Overview](#overview)
2. [Dalihan Na Tolu Foundation](#dalihan-na-tolu-three-pillars)
3. [Complete Kinship Terminology](#complete-kinship-terminology)
4. [Affinal Relationships](#comprehensive-affinal-relationships-implementation)
5. [Reciprocal Relationships](#reciprocal-relationship-logic)

### Part II: Technical Implementation
6. [Algorithm Overview](#algorithm-implementation)
7. [Detection Patterns](#detection-patterns-by-path-length)
8. [Canonical Path Encoding](#canonical-path-encoding-method)
9. [Java Implementation](#java-code-implementation)
10. [Performance Optimization](#performance-optimizations)
11. [Testing & Validation](#validation-and-testing)

---

# PART I: CULTURAL SPECIFICATION

## Overview

This document provides the definitive reference for Batak Toba kinship terminology (*partuturan*) and the algorithms used to detect these relationships in Tarombo. It serves as both a cultural preservation document and a technical specification for the relationship detection system.

The Batak Toba kinship system defines relationships not only by blood (consanguineal) but also by marriage (affinal) and ritual function, all governed by the **Dalihan Na Tolu** social framework.

## Dalihan Na Tolu (Three Pillars)

The Dalihan Na Tolu is the foundational social structure of Batak Toba society, consisting of three interconnected relationship categories that govern all social interactions:

| Group | Role | Cultural Principle |
|--------|------|------------|
| **Hula-hula** | Wife-givers (maternal line) | *Somba marhula-hula* — honor those who give wives |
| **Dongan Tubu** | Same-clan kin | *Manat mardongan tubu* — act prudently with clan peers |
| **Boru** | Wife-takers (paternal line) | *Elek marboru* — treat wife-takers with kindness |

#### 1. Hula-hula (Wife-giving lineage - Superior position)
- **Cultural Role**: Wife-giving clan holds the highest respect and authority
- **Social Principle**: *Somba marhula-hula* (Honor those who give wives)
- **Examples**: Mother's clan, wife's family, mother's brother's family
- **Ceremonial Role**: Blessing giver, highest honor in rituals
- **Social Status**: Superior position, receives utmost respect

#### 2. Dongan Tubu (Same clan relatives - Equal position)
- **Cultural Role**: Mutual support and cooperation within the same marga (clan)
- **Social Principle**: *Manat mardongan tubu* (Be wise with clan siblings)
- **Examples**: Father's clan members, patrilineal relatives, same marga families
- **Ceremonial Role**: Equal participants, shared responsibilities
- **Social Status**: Equal status, mutual respect and support

#### 3. Boru (Wife-receiving lineage - Inferior position)
- **Cultural Role**: Provides service and respect to wife-giving clans
- **Social Principle**: *Elek marboru* (Be kind to wife-receivers)
- **Examples**: Sister's husband's family, daughter's husband's family
- **Ceremonial Role**: Service provider, respectful assistance
- **Social Status**: Respectful service position, shows deference

## Complete Kinship Terminology

### Core Terms by Category

#### Hula-hula Relationships (Wife-giving lineage)
| Term | Relationship | Gender | Cultural Context |
|------|-------------|--------|------------------|
| **Tulang** | Mother's brother, Wife's father | Male | Ultimate authority figure, blessing giver |
| **Nantulang** | Mother's brother's wife, Wife's mother | Female | Respected elder woman, ceremonial guide |
| **Lae** | Mother's brother's son | Male | Male cross-cousin, close family ally |
| **Pariban** | Mother's brother's daughter | Female | Female cross-cousin, marriageable relative |
| **Amanguda*** | Mother's sister's husband | Male | Honorary uncle through marriage |
| **Nanguda*** | Mother's sister | Female | Beloved aunt, mother-like figure |

*Note: Amanguda and Nanguda can be Hula-hula when referring to mother's sister's relationships*

#### Dongan Tubu Relationships (Same clan)
| Term | Relationship | Gender | Cultural Context |
|------|-------------|--------|------------------|
| **Amanguda** | Father's brother | Male | Uncle within same clan, advisor |
| **Inanguda** | Father's brother's wife | Female | Aunt by marriage, family support |
| **Nanguda** | Father's sister, Mother's sister | Female | Paternal aunt, clan woman |
| **Haha** | Older brother | Male | Respected elder sibling |
| **Anggi** | Younger sibling | Any | Protected younger family member |
| **Dongan Tubu** | Same-generation same-marga relative | Any | Clan sibling, equal peer |

#### Boru Relationships (Wife-receiving lineage)
| Term | Relationship | Gender | Cultural Context |
|------|-------------|--------|------------------|
| **Namboru** | Father's sister | Female | Honored aunt, blessing recipient |
| **Amangboru** | Father's sister's husband, Daughter's husband's father | Male | Service recipient, honored guest |
| **Ito** | Father's sister's daughter | Female | Female cross-cousin, marriageable relative |
| **Anak Boru** | Sister's husband, Daughter's husband | Male | Service provider, respectful son-in-law |

#### Affinal Relationships (Marriage-based)
| Term | Relationship | Gender | Cultural Context |
|------|-------------|--------|------------------|
| **Parumaen** | Son's wife | Female | Daughter-in-law, family addition |
| **Hela** | Daughter's husband | Male | Son-in-law, family connection |
| **Eda** | Brother's wife | Female | Sister-in-law, extended family |
| **Lae** | Sister's husband | Male | Brother-in-law, family ally |
| **Bao** | Co-parent-in-law | Any | Fellow parent, shared responsibility |

## Comprehensive Affinal Relationships Implementation

### Overview of Affinal Patterns
Affinal relationships (marriage-based connections) form a critical component of the Batak Toba kinship system. These relationships extend the family network through marriage and create important social obligations within the Dalihan Na Tolu framework.

### 3-Person Affinal Path Patterns

#### Child-in-law Relationships
1. **Parumaen** - Son's Wife (Daughter-in-law)
   - **Pattern**: A → Child → Child's Wife
   - **Example**: Person → Son → Son's Wife
   - **Classification**: Wife Taker (Hula-hula connection)
   - **Cultural Significance**: Brings new woman into family, treated with respect
   - **Social Obligations**: Welcomed into household, receives guidance from mother-in-law

2. **Hela** - Daughter's Husband (Son-in-law)
   - **Pattern**: A → Child → Child's Husband  
   - **Example**: Person → Daughter → Daughter's Husband
   - **Classification**: Wife Giver (Boru relationship)
   - **Cultural Significance**: Creates new Boru connection for family
   - **Social Obligations**: Provides service to wife's family, shows respect

#### Sibling-in-law Relationships
3. **Eda** - Brother's Wife (Sister-in-law)
   - **Pattern**: A → Brother → Brother's Wife
   - **Example**: Person → Brother → Brother's Wife
   - **Classification**: Wife Taker relationship
   - **Cultural Significance**: Extended family member through brother's marriage
   - **Social Obligations**: Treated as family member, mutual respect expected

4. **Lae** - Sister's Husband (Brother-in-law) 
   - **Pattern**: A → Sister → Sister's Husband
   - **Example**: Person → Sister → Sister's Husband
   - **Classification**: Wife Giver relationship
   - **Cultural Significance**: Creates connection to sister's husband's family
   - **Social Obligations**: Respectful relationship, potential ceremonial cooperation

#### Extended Parent-Sibling Patterns
5. **Namboru** - Father's Sister (Paternal Aunt)
   - **Pattern**: A → Father → Father's Sister
   - **Example**: Person → Father → Father's Sister
   - **Classification**: Boru (Wife Taker) - woman who left the clan through marriage
   - **Cultural Significance**: Father's sister who married out, maintains clan connection
   - **Social Obligations**: Receives respect as clan woman, blessing giver

6. **Tulang Rorobot** - Mother's Brother's Brother (Distant Uncle)
   - **Pattern**: A → Mother → Mother's Brother's Brother
   - **Example**: Person → Mother → Mother's Brother's Brother
   - **Classification**: Distant Hula-hula
   - **Cultural Significance**: Extended maternal uncle, less direct authority than Tulang
   - **Social Obligations**: Respected elder, ceremonial participant

### 4-Person Affinal Path Patterns

#### Parent's Sibling's Spouse
1. **Amangboru** - Father's Sister's Husband
   - **Pattern**: A → Father → Father's Sister → Sister's Husband
   - **Example**: Person → Father → Father's Sister → Father's Sister's Husband
   - **Classification**: Boru relationship
   - **Cultural Significance**: Married to father's sister (Namboru), receives service
   - **Social Obligations**: Honored position, receives respect and service

2. **Nantulang** - Mother's Brother's Wife (Extended)
   - **Pattern**: A → Mother → Mother's Brother → Brother's Wife
   - **Example**: Person → Mother → Mother's Brother → Mother's Brother's Wife
   - **Classification**: Hula-hula (Wife-giving)
   - **Cultural Significance**: Wife of Tulang (mother's brother), honored woman
   - **Social Obligations**: Highly respected, ceremonial guidance provider

#### Co-parental Relationships
3. **Bao** - Child's Spouse's Parent (Co-parent-in-law)
   - **Pattern**: A → Child → Child's Spouse → Spouse's Parent
   - **Example**: Person → Child → Child's Spouse → Child's Spouse's Parent
   - **Classification**: Reciprocal affinal relationship
   - **Cultural Significance**: Parents of children who married each other
   - **Social Obligations**: Mutual respect, shared ceremonial responsibilities
   - **Special Status**: Unique equal-standing relationship despite Dalihan Na Tolu hierarchy

#### Extended Spouse's Family
4. **Tulang** - Spouse's Father (Extended patterns)
   - **Pattern**: A → Sibling → Sibling's Spouse → Spouse's Father
   - **Example**: Person → Sister → Sister's Husband → Sister's Husband's Father
   - **Classification**: Hula-hula (Wife Giver family)
   - **Cultural Significance**: Father of sibling's spouse, source of family connection
   - **Social Obligations**: High respect, blessing recipient

### Technical Implementation Details

#### Core Algorithm Files
The affinal relationship detection is implemented in:
- **`app/src/main/java/app/familygem/RelationshipUtils.java`**
  - Enhanced `analyze3PersonPath()` method with affinal pattern detection
  - Enhanced `analyze4PersonPath()` method with complex affinal relationships
  - Added `isChild()` helper method for accurate relationship detection
  - Comprehensive logging system for debugging relationship patterns

#### Gender Detection and Fallback Logic
The system includes sophisticated gender detection:
- **Primary Source**: GEDCOM SEX tags from genealogical data
- **Fallback Logic**: Name-based gender inference when GEDCOM tags missing
- **Cultural Context**: Uses traditional Batak naming patterns
- **Debug Support**: Extensive logging to track gender detection process

#### BFS Tree Traversal Algorithm
The relationship detection uses Breadth-First Search:
- **Path Discovery**: Finds shortest relationship paths between any two people
- **Multi-pattern Support**: Analyzes 3-person and 4-person relationship paths
- **Bidirectional Analysis**: Handles both forward and reverse path analysis
- **Performance**: Optimized with iteration limits and visited tracking

### Dalihan Na Tolu Integration in Affinal Relationships

#### Three Pillars Classification for Affinal Terms

**Hula-hula (Wife Givers) - Affinal Connections:**
- **Tulang**: Mother's brother, also wife's father
- **Nantulang**: Mother's brother's wife, wife's mother  
- **Lae**: Sister's husband (gives sister as wife to another family)
- **Bao**: Co-parent when child marries into wife-giving family

**Dongan Tubu (Same Clan) - Affinal Extensions:**
- **Amanguda**: Father's brother, maintains clan solidarity
- **Inanguda**: Father's brother's wife, clan support
- **Eda**: Brother's wife, brought into clan

**Boru (Wife Takers) - Affinal Service:**
- **Namboru**: Father's sister (clan woman who married out)
- **Amangboru**: Father's sister's husband (receives service)
- **Hela**: Daughter's husband, provides service
- **Anak Boru**: Sister's husband (alternative use)

### Cultural Authenticity Verification

#### Source Validation
- **Primary Sources**: Traditional Partuturan documents
- **Community Validation**: Verified by Batak Toba cultural experts
- **Elder Consultation**: Confirmed with community elders
- **Historical Cross-reference**: Checked against anthropological studies

#### Relationship Classification Consistency
- **Reciprocal Logic**: Ensures mutual relationship terms are correct
- **Gender Accuracy**: Proper masculine/feminine term assignment
- **Hierarchical Respect**: Maintains proper Dalihan Na Tolu positioning
- **Cultural Context**: Each term includes appropriate social obligations

### Usage in Application

When Batak Toba kinship system is selected:
1. **Comprehensive Detection**: Both consanguineal (blood) and affinal (marriage) relationships detected
2. **Cultural Accuracy**: Authentic Batak terminology used throughout
3. **Complex Networks**: Multi-generational and extended family relationships properly classified
4. **Dalihan Na Tolu Context**: Each relationship classified within three-pillar framework
5. **Educational Value**: Relationship explanations include cultural significance

### Testing and Validation

#### Comprehensive Test Coverage
- **Pattern Testing**: All 3-person and 4-person affinal patterns verified
- **Edge Cases**: Unusual family configurations tested
- **Gender Logic**: Masculine/feminine term assignment validated
- **Cultural Accuracy**: Community review and expert validation

#### Debug and Logging Support
- **Relationship Tracing**: Comprehensive logs show detection process
- **Path Analysis**: Debug output shows genealogical paths analyzed
- **Gender Detection**: Logs track gender determination logic
- **Fallback Handling**: Graceful handling of incomplete GEDCOM data

### Future Enhancements

#### Potential Additions
- **Audio Pronunciation**: Proper pronunciation guides for Batak terms
- **Ceremonial Context**: Specific adat ceremony role explanations
- **Extended Network**: Visualization of full relationship network
- **Cultural Education**: Interactive learning about Dalihan Na Tolu
- **Multilingual Support**: Additional language options beyond English/Batak

#### Direct Family Terms
| Term | Relationship | Gender | Cultural Context |
|------|-------------|--------|------------------|
| **Ama** | Father | Male | Patriarch, clan leader |
| **Ina** | Mother | Female | Matriarch, family heart |
| **Anak** | Child | Any | Beloved offspring, future generation |

## Marga-Based Generational Relationships

### Cultural Principle: Same Marga, Different Generations

In Batak Toba society, the **marga** (patrilineal clan name/surname) carries profound significance beyond mere family identification. When two individuals share the same marga but are not closely related by direct blood lineage, their relationship terminology is determined by **generational position** rather than by distant clan connection.

This principle reflects the deep cultural understanding that all members of the same marga descend from a common ancestor and maintain kinship obligations even across many generations of separation.

### The Marga Generational Hierarchy

When encountering someone of the same marga without a known close genealogical connection:

#### One Generation Above (Senior Marga Member)
**Kinship Term**: **Amanguda** or **Amangtua** (if older than father)

- **Cultural Rationale**: A same-marga person one generation above occupies the same social position as one's father's brothers
- **Example**: If Arnold Siboro meets Kardi Siboro (one generation older), Kardi is called "Amanguda" or "Amangtua"
- **Social Obligations**: 
  - Show respect as to father's brother
  - Seek counsel and blessing when appropriate
  - Maintain Dongan Tubu solidarity (same clan cooperation)
- **Ceremonial Role**: Can act as clan representative in family ceremonies

**Key Distinction**: This person is **NOT** called "Dongan Sahala" (distant clan relative), but rather receives the full kinship term appropriate to their generational position.

#### Same Generation (Contemporary Marga Member)
**Kinship Term**: **Dongan Tubu** (Clan Sibling)

- **Cultural Rationale**: Same-generation marga members are considered clan siblings within the same patrilineal line
- **Example**: If Arnold Siboro meets Jonathan Siboro (same generation), they are "Dongan Tubu"
- **Social Obligations**:
  - Mutual support and cooperation
  - Equal status and respect
  - Clan solidarity and brotherhood
- **Ceremonial Role**: Work together as peers in clan activities

#### One Generation Below (Junior Marga Member)
**Kinship Term**: **Bere** (as nephew/niece)

- **Cultural Rationale**: A same-marga person one generation below occupies the position of sibling's child
- **Example**: If Arnold Siboro meets a young Siboro (one generation younger), they are called "Bere"
- **Social Obligations**:
  - Provide guidance and protection
  - Act as uncle/aunt figure
  - Uphold family honor and values
- **Ceremonial Role**: Blessing giver, mentor

### Implementation Principles

1. **Genealogical Path Priority**: If a specific genealogical connection is known (e.g., actual cousin through documented lineage), use the precise relationship term
2. **Marga Matching Fallback**: When no specific genealogical path exists but margas match, apply generational rules
3. **Generation Determination Strategy** (in order of preference):
   - **Common Ancestor Path**: Find nearest common ancestor and compare distances from each person to that ancestor
   - **Absolute Tree Depth**: Count generations from oldest known ancestor for each person
   - **Same Generation Default**: If tree structure is insufficient, assume same generation (Dongan Tubu for same marga)
4. **Cultural Respect**: The marga generational system ensures no clan member is dismissed as merely "distant" - all maintain kinship obligations

**Technical Implementation:**
- The `findNearestCommonAncestor()` method performs BFS traversal to find the closest shared ancestor
- Generational difference = (distance of person B to ancestor) - (distance of person A to ancestor)
- Positive result means B is older generation, negative means B is younger, zero means same generation
- This approach is culturally authentic as it follows actual family tree paths, not arbitrary metrics like birth year
- **Note**: Birth year comparison is NOT used as it doesn't respect genealogical structure in Batak kinship

### Example Scenarios

| Ego | Other Person | Marga Match | Generation Diff | Correct Term | Wrong Term |
|-----|-------------|-------------|-----------------|--------------|------------|
| Arnold Siboro | Kardi Siboro | Yes | +1 (Kardi older) | **Amanguda** | ~~Dongan Sahala~~ |
| Arnold Siboro | Jonathan Siboro | Yes | 0 (same) | **Dongan Tubu** | ~~Dongan Sahala~~ |
| Arnold Siboro | Young Siboro | Yes | -1 (younger) | **Bere** | ~~Dongan Sahala~~ |
| Arnold Siboro | Elder Hutabarat | No | +1 | Dongan Sahala | n/a (different marga) |

### Cultural Significance

This system ensures:
- **Clan Unity**: All marga members maintain active kinship bonds
- **Social Structure**: Clear hierarchical positioning based on generation
- **Respect Protocols**: Appropriate address and behavior toward all clan members
- **Ceremonial Participation**: Proper roles in adat (traditional) ceremonies
- **Inheritance Rights**: Understanding of potential clan property/ritual obligations

The marga-generational system is fundamental to Batak Toba identity and must be properly implemented in any genealogical system claiming to represent authentic *partuturan* (kinship terminology).

## Reciprocal Relationship Logic

Understanding reciprocal relationships is essential for proper kinship term application. When person A calls person B by a specific term, person B uses the reciprocal term for person A:

| Term (A → B) | Reciprocal (B → A) | Relationship Type |
|--------------|-------------------|-------------------|
| **Tulang** (Uncle) | **Bere** (Nephew/Niece) | Maternal uncle ↔ Sister's child |
| **Namboru** (Aunt) | **Bere** (Nephew/Niece) | Paternal aunt ↔ Brother's child |
| **Amangboru** (Uncle by marriage) | **Ito** (Cousin) | Father's sister's husband ↔ Niece/Nephew |
| **Inanguda** (Aunt) | **Bere** (Nephew/Niece) | Affinal aunt ↔ Niece/nephew |
| **Parumaen** (Daughter-in-law) | **Hela** (Son-in-law) | Son's wife ↔ Daughter's husband |
| **Bao** (Co-parent-in-law) | **Bao** (Co-parent-in-law) | Symmetric - parents of married couple |
| **Lae** (Cousin/Brother-in-law) | **Lae** (Cousin/Brother-in-law) | Symmetric - Tulang's son or sister's husband |
| **Eda** (Sister-in-law) | **Eda** (Sister-in-law) | Symmetric - Brother's wife |
| **Haha** (Elder sibling) | **Anggi** (Younger sibling) | Age-based sibling relationship |
| **Ompu** (Grandparent) | **Pahompu** (Grandchild) | Generational reciprocal |

### Symmetric Relationships
Some relationships are naturally symmetric, meaning both parties use the same term:
- **Bao**: Co-parents-in-law call each other Bao
- **Pariban**: Cross-cousins (marriageable cousins from mother's brother or father's sister) use mutual term
- **Dongan Tubu**: Clan siblings of same generation and same marga

---

# PART II: TECHNICAL IMPLEMENTATION

## Algorithm Implementation

### Detection Patterns by Path Length

#### 2-Person Direct Relationships
```
Pattern: A ↔ B (direct connection)

Spouse Detection:
- areSpouses(A, B) → "Husband/Wife"

Parent-Child Detection:
- isParent(A, B) → "Father/Mother" (A is parent)
- isChild(A, B) → "Son/Daughter" (A is child)

Sibling Detection:
- areSiblings(A, B) → "Brother/Sister" or "Haha/Anggi"
```

#### 3-Person Pattern Analysis
```
Pattern: A → Connector → B

Parent's Sibling:
A → Parent → Parent's Sibling
- Father's brother → "Amanguda (Dongan Tubu)"
- Father's sister → "Namboru (Boru)"
- Mother's brother → "Tulang (Hula-hula)"
- Mother's sister → "Nanguda (Hula-hula)"

Sibling's Spouse:
A → Sibling → Sibling's Spouse
- Brother's wife → "Eda"
- Sister's husband → "Lae"

Child's Spouse:
A → Child → Child's Spouse
- Son's wife → "Parumaen"
- Daughter's husband → "Hela"
```

#### 4-Person Pattern Analysis
```
Pattern: A → Connector1 → Connector2 → B

Parent's Sibling's Spouse:
A → Parent → Parent's Sibling → Sibling's Spouse
- Father's sister's husband → "Amangboru (Boru)"
- Mother's brother's wife → "Nantulang (Hula-hula)"
- Father's brother's wife → "Inanguda (Dongan Tubu)"
- Mother's sister's husband → "Amanguda (Hula-hula)"

Spouse's Sibling's Child:
A → Spouse → Spouse's Sibling → Sibling's Child
- Wife's sister's son → "Child through Hula-hula connection"
- Husband's brother's daughter → "Child through Dongan Tubu connection"

Co-parent-in-law:
A → Child → Child's Spouse → Spouse's Parent
- Child's spouse's father/mother → "Bao"
```

#### 5+ Person Pattern Analysis (Sibling Inheritance)
```
Pattern: A → A's Sibling → [Relationship Path] → B

Sibling Inheritance Logic:
1. Detect if A and second person are siblings
2. Create subpath from sibling to target
3. Analyze subpath for relationship
4. Inherit relationship term from sibling

Example: Gunawi → Gunadi → Rose → Leries → Arnold
- Gunawi and Gunadi are brothers (siblings)
- Analyze: Gunadi → Rose → Leries → Arnold
- Result: "Amanguda (Mother's Sister's Husband)"
- Gunawi inherits same "Amanguda" relationship
```

### Canonical Path Encoding Method

An alternative algorithmic approach uses canonical path encoding for pattern matching. This method is particularly useful for standardized relationship detection across different genealogical systems.

#### Path Representation
Paths are represented as ordered role tokens:
- `mother`, `father`, `brother`, `sister`, `son`, `daughter`, `husband`, `wife`, `parent`, `child`, `spouse`

#### Example Encodings
```
ego → mother → brother = ["mother", "brother"]
ego → father → sister → husband = ["father", "sister", "husband"]
ego → son → wife = ["son", "wife"]
ego → daughter → husband = ["daughter", "husband"]
ego → child → spouse → parent = ["child", "spouse", "parent"]
```

#### Canonical Mapping Table
| Pattern | Batak Term | Dalihan Na Tolu Group | Type |
|---------|-----------|----------------------|------|
| `[mother, brother]` | Tulang | Hula-hula | Consanguineal |
| `[father, sister]` | Namboru | Boru | Consanguineal |
| `[father, sister, husband]` | Amangboru | Boru | Affinal |
| `[father, brother, wife]` | Inanguda | Dongan Tubu | Affinal |
| `[mother, sister]` | Inanguda / Nanguda | Dongan Tubu / Hula-hula | Consanguineal |
| `[son, wife]` | Parumaen | Hula-hula | Affinal |
| `[daughter, husband]` | Hela | Boru | Affinal |
| `[child, spouse, parent]` | Bao | Reciprocal | Affinal |
| `[mother, brother, wife]` | Nantulang | Hula-hula | Affinal |
| `[mother, brother, brother]` | Tulang Rorobot | Hula-hula | Consanguineal |
| `[mother, brother, son]` | Lae | Hula-hula | Consanguineal |
| `[father, sister, daughter]` | Ito | Boru | Consanguineal |

#### Pseudocode Implementation
```python
def get_batak_term(ego, target, genealogy_api):
    """
    Determine Batak kinship term using canonical path encoding
    
    Args:
        ego: The reference person
        target: The person to find relationship for
        genealogy_api: API to traverse genealogical relationships
    
    Returns:
        Batak kinship term string
    """
    # Find shortest path between ego and target
    path = find_shortest_relation_path(ego, target, genealogy_api)
    
    # Convert path to canonical signature
    signature = canonicalize(path, genealogy_api)
    
    # Pattern matching
    if signature == ["mother", "brother"]:
        return "Tulang"
    elif signature == ["father", "sister"]:
        return "Namboru"
    elif signature == ["father", "sister", "husband"]:
        return "Amangboru"
    elif signature == ["father", "brother", "wife"]:
        return "Inanguda"
    elif signature == ["mother", "sister"]:
        return "Nanguda"
    elif signature == ["son", "wife"]:
        return "Parumaen"
    elif signature == ["daughter", "husband"]:
        return "Hela"
    elif signature == ["child", "spouse", "parent"]:
        return "Bao"
    elif signature == ["mother", "brother", "wife"]:
        return "Nantulang"
    elif signature == ["mother", "brother", "brother"]:
        return "Tulang Rorobot"
    elif signature == ["mother", "brother", "son"]:
        return "Lae"
    elif signature == ["father", "sister", "daughter"]:
        return "Ito"
    else:
        return "Unknown"
```

### Cultural Logic Implementation

#### Java Code Implementation
```java
// Example algorithm for parent's sibling detection
if (isParent(connector, A) && areSiblings(connector, B)) {
    Gender parentGender = getGender(connector);
    Gender siblingGender = getGender(B);
    
    if (parentGender == MALE && siblingGender == MALE) {
        return "Amanguda (Father's Brother - Dongan Tubu)";
    } else if (parentGender == MALE && siblingGender == FEMALE) {
        return "Namboru (Father's Sister - Boru)";
    } else if (parentGender == FEMALE && siblingGender == MALE) {
        return "Tulang (Mother's Brother - Hula-hula)";
    } else if (parentGender == FEMALE && siblingGender == FEMALE) {
        return "Nanguda (Mother's Sister - Hula-hula)";
    }
}
```

#### Dalihan Na Tolu Classification Logic
```java
// Automatic cultural classification
String getRelationshipWithContext(String baseTerm, RelationshipType type) {
    switch (type) {
        case MOTHERS_SIDE:
            return baseTerm + " - Hula-hula (Wife-giving, superior)";
        case FATHERS_SIDE_SAME_CLAN:
            return baseTerm + " - Dongan Tubu (Same clan, equal)";
        case SISTERS_HUSBANDS_SIDE:
            return baseTerm + " - Boru (Wife-receiving, inferior)";
        default:
            return baseTerm;
    }
}
```

#### Sibling Inheritance Algorithm
```java
// Sibling relationship inheritance
if (path.size() == 5 && areSiblings(path.get(0), path.get(1))) {
    // Create 4-person subpath from sibling
    List<Person> siblingPath = path.subList(1, 5);
    
    // Analyze sibling's relationship
    String siblingRelationship = analyze4PersonPath(siblingPath);
    
    if (siblingRelationship != null) {
        // Inherit the same relationship
        return siblingRelationship;
    }
}
```

## Cultural Principles in Code

### Respect Hierarchy Implementation
```java
// Ensure proper respect levels in relationship terms
String formatRelationshipWithRespect(String term, DalihanNaToluType type) {
    switch (type) {
        case HULA_HULA:
            return term + " (to be highly respected)";
        case DONGAN_TUBU:
            return term + " (to be treated as equal)";
        case BORU:
            return term + " (to be treated with kindness)";
    }
}
```

### Gender Cultural Logic
```java
// Gender-appropriate term selection
String getGenderAppropriateTerm(String relationship, Gender gender) {
    // Ensures cultural accuracy in gender-specific terminology
    // Example: Different terms for male vs female parent's siblings
    return culturallyAccurateTerm;
}
```

## Performance Optimizations

### BFS Algorithm Efficiency
- **Iteration Limit**: Maximum 100 iterations to prevent infinite loops
- **Visited Tracking**: Prevents cycles in family tree traversal
- **Early Termination**: Stops when target relationship found

### Memory Management
- **Path Reuse**: Efficient list manipulation for path analysis
- **Garbage Collection**: Proper cleanup of temporary objects
- **Caching**: Store frequently accessed relationships

### Cultural Fast-Paths
- **Sibling Detection**: Quick identification prevents deep analysis
- **Direct Relationships**: Immediate detection of spouse/parent/child
- **Common Patterns**: Optimized detection for frequent relationships

## Validation and Testing

### Cultural Accuracy Tests
- **Expert Validation**: Verified with Batak Toba cultural consultants
- **Community Review**: Confirmed by Batak community members
- **Historical Sources**: Cross-referenced with traditional texts

### Algorithm Testing
- **Edge Cases**: Unusual family configurations
- **Performance**: Large family networks (1000+ people)
- **Consistency**: Sibling inheritance verification
- **Gender Logic**: Proper term assignment

### Real-world Scenarios
- **Modern Families**: Divorced, remarried, adopted relationships
- **Traditional Structures**: Complex multi-generational families
- **Mixed Heritage**: Families with multiple cultural backgrounds

### XML String Resources for Android

For Android app localization, kinship terms are mapped to string resources:

```xml
<!-- Hula-hula (Wife-giving) relationships -->
<string name="rel_batak_mothers_brother">Tulang</string>
<string name="rel_batak_mothers_brothers_wife">Nantulang</string>
<string name="rel_batak_mothers_brothers_brother">Tulang Rorobot</string>
<string name="rel_batak_mothers_brothers_son">Lae</string>
<string name="rel_batak_mothers_sister">Nanguda</string>

<!-- Dongan Tubu (Same clan) relationships -->
<string name="rel_batak_fathers_brother">Amanguda</string>
<string name="rel_batak_fathers_brothers_wife">Inanguda</string>
<string name="rel_batak_elder_sibling">Haha</string>
<string name="rel_batak_younger_sibling">Anggi</string>

<!-- Boru (Wife-receiving) relationships -->
<string name="rel_batak_fathers_sister">Namboru</string>
<string name="rel_batak_fathers_sisters_husband">Amangboru</string>
<string name="rel_batak_fathers_sisters_daughter">Ito</string>

<!-- Affinal relationships -->
<string name="rel_batak_sons_wife">Parumaen</string>
<string name="rel_batak_daughters_husband">Hela</string>
<string name="rel_batak_brothers_wife">Eda</string>
<string name="rel_batak_sisters_husband">Lae</string>
<string name="rel_batak_childs_spouses_parent">Bao</string>

<!-- Direct family -->
<string name="rel_batak_father">Ama</string>
<string name="rel_batak_mother">Ina</string>
<string name="rel_batak_child">Anak</string>
<string name="rel_batak_grandparent">Ompu</string>
<string name="rel_batak_grandchild">Pahompu</string>
</xml>
```

### Unit Test Examples

```python
# Consanguineal relationship tests
assert get_batak_term(ego, ego.mother.brother) == "Tulang"
assert get_batak_term(ego, ego.father.sister) == "Namboru"
assert get_batak_term(ego, ego.father.brother) == "Amanguda"
assert get_batak_term(ego, ego.mother.sister) == "Nanguda"

# Affinal relationship tests
assert get_batak_term(ego, ego.father.sister.husband) == "Amangboru"
assert get_batak_term(ego, ego.father.brother.wife) == "Inanguda"
assert get_batak_term(ego, ego.mother.brother.wife) == "Nantulang"
assert get_batak_term(ego, ego.son.wife) == "Parumaen"
assert get_batak_term(ego, ego.daughter.husband) == "Hela"

# Co-parent-in-law test
assert get_batak_term(ego, ego.child.spouse.parent) == "Bao"

# Reciprocal relationship tests
assert get_batak_term(uncle, nephew) == "Bere"
assert get_batak_term(nephew, uncle) == "Tulang"
assert get_batak_term(bao1, bao2) == "Bao"
assert get_batak_term(bao2, bao1) == "Bao"  # Symmetric
```

## Conclusion

This Batak Toba kinship system implementation preserves authentic cultural knowledge while providing robust algorithmic detection. It serves as both a practical tool for genealogical applications and a digital preservation of traditional Batak Toba family wisdom.

The system respects the profound cultural significance of the Dalihan Na Tolu framework while adapting to modern technological requirements, ensuring that traditional knowledge remains accessible and accurate for future generations.

---

**Cultural Consultants**: Batak Toba Community Elders  
**Technical Implementation**: Arnold Siboro  
**Version**: 3.0 - Complete Cultural Specification and Technical Implementation  
**Last Updated**: 2025-11-16 (Merged with kinship examples and affinal relationships documentation)

---

## Document History

- **2025-10-31**: Initial creation with core terminology and algorithms
- **2025-10-31**: Merged comprehensive affinal relationships documentation
- **2025-11-16**: Merged kinship examples; restructured into Cultural Specification (Part I) and Technical Implementation (Part II); added reciprocal relationships, canonical path encoding, XML resources, and unit test examples