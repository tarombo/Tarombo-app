# Tarombo/FamilyGem Copilot Instructions

## Project Overview
**Tarombo** is an Android genealogy app for managing family trees. It's a fork of Family Gem, written in Java with a strong focus on GEDCOM 5.5.1 standard compliance. Data is stored internally as JSON (via `JsonParser`) and can be imported/exported as GEDCOM files.

- **Min SDK:** 24 (Android 7.0) | **Target SDK:** 34 | **Java:** 1.8
- **Key dependency:** `org.familysearch.gedcom:gedcom:1.11.0` for GEDCOM parsing
- **Build tool:** Gradle 7.3.3 with Android Plugin 7.2.2

## Architecture Patterns

### Global State Management
- **`Global` class** is the Application singleton holding:
  - `gc` (Gedcom) - currently opened tree
  - `settings` (Settings) - user preferences in JSON (`settings.json`)
  - `indi` (String) - ID of the selected person displayed across UI
  - `edited` (boolean) - flag for activity-level state changes
  
- Settings persist to `context.getFilesDir()/settings.json`. Migration from Italian to English keys happens in `Global.updateSettings()`.

### Data Persistence Strategy
- **Internal format:** JSON via `JsonParser` (org.folg.gedcom package)
- **Tree storage:** Each tree stored as a separate JSON file by ID in `context.getFilesDir()`
- **Backup format:** ZIP files containing GEDCOM + media
- **JSON-to-Gedcom conversion:** `new JsonParser().fromJson(jsonString)` / `jp.toJson(gedcom)`
- **GEDCOM file I/O:** `GedcomWriter` (for export) & `ModelParser().parseGedcom()` (for import)

Example:
```java
// In Alberi.java
Gedcom gc = new JsonParser().fromJson(jsonString);  // Load tree
String json = new JsonParser().toJson(gc);           // Save tree
```

### Object Mapping (Wrapper Pattern)
**`Memoria` class** maintains static singleton mapping between GEDCOM model classes and custom UI wrappers:
```
Person → Individuo
Family → Famiglia  
Media → Immagine
Source → Fonte
Name → Nome
EventFact → Evento
Address → Indirizzo
```
This dual-class pattern bridges data model and presentation. Wrapper classes live in `dettaglio/` package.

### Fragment-Based Navigation
**`Principal` (main activity)** manages 8 core fragments:
- `Diagram` - family tree visualization
- `Anagrafe` - persons list
- `Chiesa` - families
- `Galleria` - media gallery
- `Quaderno` - notes
- `Biblioteca` - sources
- `Magazzino` - repositories (archives)
- `Podio` - submitters (authors)

Navigation drawer switches fragments via `FragmentManager`. Each fragment operates on global `Global.gc`.

### Comparison & Merge System
- **`CompareDiffTree.compare()`** - returns `List<DiffPeople>` with change type (ADDED/MODIFIED/REMOVED)
- **`TreeSplitter.split()`** - splits tree from a fulcrum person for sharing workflow
- Used in: `CompareChangesActivity`, `ReviewChangesActivity`, collaboration features

## Key Conventions

### Italian → English Migration
Code comments are mostly Italian. Settings and configuration keys migrated to English in v0.8. When modifying `Global.updateSettings()`, maintain backward compatibility:
```java
json = json.replace("\"vecchiaNome\":", "\"newName\":");
```

### ID Generation & Uniqueness
- GEDCOM IDs like `I1*684d96e5-b24e-4684-be6f-eb6f9626de6e` append UUIDs for conflict prevention
- Helper method: `Helper.makeGuidGedcom(gedcom)` in oauthLibGithub module

### External Integration
- **GitHub sync:** `oauthLibGithub/` module handles tree sharing via GitHub API (fork/PR model)
  - Tree stored as `tree.json` + `info.json` in a GitHub repo
  - `FamilyGemTreeInfoModel` serializes tree metadata
  - Classes: `CreateRepoTask`, `RefreshRepoTask`, `MergePRTask`
- **File provider:** Uses AndroidX `FileProvider` for media file access
- **FTP upload:** `Condivisione` for legacy FTP-based sharing via Apache Commons Net

### Build Configuration
- **Signing:** Configured in `secrets.properties` (not committed)
- **Build flavors:** 
  - `debug` - testing ads (ID: `ca-app-pub-3940...`)
  - `debug (No Ads)` - no ad display
  - `release` - production ads (ID: `ca-app-pub-5341...`)
- **Crash reporting:** Firebase Crashlytics enabled in release builds
- **Multi-dex:** Required; enabled in `build.gradle`

## Developer Workflows

### Building
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires secrets.properties with signing keys)
./gradlew assembleRelease

# Run tests (includes GEDCOM/JSON conversion tests)
./gradlew connectedAndroidTest
```

### Testing Key Classes
- **`DiffTest.java`** - JSON parsing, tree comparison scenarios (A–E)
- **`SplitTreeTest.java`** - tree splitting and media handling
- **`PrivacyTest.java`** - private person filtering
- **`ImportTest.java`** - GEDCOM file import

Run tests:
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.familygem.DiffTest
```

### Debugging GEDCOM Issues
1. Parse test GEDCOM: `new ModelParser().parseGedcom(inputStream)`
2. Convert to JSON: `new JsonParser().toJson(gedcom)`
3. Check IDs: `gedcom.createIndexes()` then `gedcom.getPerson(id)`
4. Inspect differences: `CompareDiffTree.compare(gedcom1, gedcom2)`

## Common Pitfalls

1. **Settings not persisted:** Call `Global.settings.save()` after mutations
2. **Stale UI after editing:** Set `Global.edited = true` so parent activities refresh
3. **Tree not loaded:** Check `Global.gc` is not null; use `Alberi.leggiJson(treeId)` to load
4. **Media paths invalid:** Use `FileProvider` for sharing; raw file paths break on modern Android
5. **GEDCOM parsing fails:** Validate XML structure first; use test data in `androidTest/assets/`

## Module Structure
- **`app/`** - main Android application
  - `src/main/java/app/familygem/` - core logic + fragments
  - `src/main/res/` - layouts, strings (translations on Weblate), drawables
  - `src/androidTest/` - instrumented tests with JSON test fixtures
  - `build.gradle` - Firebase, GMS, Gedcom dependency
  
- **`oauthLibGithub/`** - GitHub OAuth & tree sync (separate module)
  - REST API calls via Retrofit
  - Task-based async operations for sync workflow
  
- **`lab/`** - experimental/lab features (not in main release)

## Resources
- GEDCOM standard: https://www.familysearch.org/developers/docs/gedcom/5.5.1
- Gedcom5-Java library: https://github.com/FamilySearch/gedcom5-java
- Weblate translations: https://hosted.weblate.org/projects/tarombo/app/
- Issues/feedback: GitHub issues or tarombo-app@googlegroups.com
