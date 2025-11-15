# Changelog

All notable changes to Tarombo will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2024-10-31

### Added - Comprehensive Batak Toba Kinship System

#### 🎉 Major Cultural Feature Implementation
- **Complete Batak Toba kinship system** based on authentic Dalihan Na Tolu framework
- **Culturally accurate relationship terminology** with proper social context
- **Advanced relationship detection algorithms** using sophisticated graph traversal
- **Sibling inheritance logic** ensuring consistent relationship terms

#### 🏛️ Cultural Framework (Dalihan Na Tolu)
- **Hula-hula relationships** (wife-giving lineage - superior position)
  - Tulang (Mother's brother, Wife's father)
  - Nantulang (Mother's brother's wife, Wife's mother)
  - Amanguda (Mother's sister's husband - Hula-hula variant)
- **Dongan Tubu relationships** (same clan relatives - equal position)
  - Amanguda (Father's brother, Mother's sister's husband)
  - Inanguda (Father's brother's wife, Mother's sister)
  - Nanguda (Mother's sister, Father's brother's wife)
- **Boru relationships** (wife-receiving lineage - inferior position)
  - Amangboru (Father's sister's husband, Daughter's husband's father)
  - Anak Boru (Sister's husband, Daughter's husband)
- **Co-parental relationships**
  - Bao (Co-parent-in-law relationships)

#### 🔧 Technical Implementation
- **Breadth-First Search (BFS) engine** for connection path discovery
- **Multi-pattern path analysis** supporting 2, 3, 4, and 5+ person relationships
- **Performance optimization** with ANR prevention for mobile stability
- **Memory efficient algorithms** with visited node tracking
- **Gender-sensitive cultural logic** for authentic terminology

#### 🚀 Advanced Features
- **Sibling relationship inheritance** - brothers/sisters share same kinship terms
- **Complex affinal relationship detection** through multi-step family connections
- **Authentic cultural classification** with proper Dalihan Na Tolu categorization
- **Performance-optimized graph traversal** handling large family networks
- **Fast sibling detection** preventing unnecessary deep analysis

#### 📱 User Experience Enhancements
- **"Show" button integration** for relationship diagrams
- **Cultural context display** showing social position and clan relationships
- **Authentic Batak Toba terminology** with English explanations
- **Comprehensive relationship coverage** from simple to complex family patterns

#### 🛠️ Bug Fixes and Improvements
- **ANR crash prevention** in complex family relationship calculations
- **Spouse relationship detection** for married couples
- **Fragment navigation stability** in Android environment
- **Touch event handling** improvements for relationship interaction
- **Build system compatibility** with Android Gradle Plugin 8.x

#### 📚 Documentation
- **Comprehensive technical documentation** in `BATAK_KINSHIP_SYSTEM.md`
- **Cultural heritage preservation** through digital kinship system
- **Algorithm explanation** and implementation details
- **Updated README** with cultural feature highlights
- **Inline code documentation** for maintenance and extension

#### 🔬 Testing and Validation
- **Extensive relationship pattern testing** across multiple family scenarios
- **Cultural accuracy validation** with authentic Batak Toba principles
- **Performance stress testing** with large family networks
- **Sibling consistency verification** ensuring proper inheritance
- **Cross-generational relationship verification** maintaining cultural rules

### Technical Details

#### Performance Optimizations
- BFS iteration limit (100) prevents infinite loops and ANR crashes
- Fast sibling detection reduces unnecessary path analysis
- Memory-efficient visited node tracking
- Optimized path manipulation for complex relationships

#### Cultural Accuracy Features
- Gender-sensitive relationship term selection
- Proper Dalihan Na Tolu social position classification
- Sibling inheritance maintaining cultural consistency
- Authentic terminology with cultural context explanations

#### Supported Relationship Patterns
- **Direct relationships**: spouse, parent-child, siblings
- **3-person patterns**: parent's siblings, sibling's spouses, child's spouses
- **4-person patterns**: complex affinal relationships, co-parent-in-laws
- **5+ person patterns**: sibling inheritance, extended cultural connections

### Breaking Changes
- None - All changes are additive feature enhancements

### Developer Notes
- New `RelationshipUtils` class implements all kinship logic
- Comprehensive logging for debugging relationship detection
- Modular design allows for future cultural system expansions
- Performance monitoring integrated for mobile optimization

---

This release represents a major milestone in digital preservation of Batak Toba cultural heritage while maintaining the high technical standards expected in modern genealogy applications.

**Cultural Significance**: This implementation serves as both a practical genealogy tool and a digital preservation of traditional Batak Toba kinship knowledge, ensuring cultural heritage remains accessible to future generations.

**Technical Achievement**: Successfully combines advanced computer science algorithms with authentic cultural logic, demonstrating how technology can respectfully preserve and enhance cultural traditions.