# Private Tree Specification

## Purpose
- Protect sensitive person facts/media when syncing a shared GitHub-backed tree while keeping the person nodes visible for graph integrity.
- Support owner-only storage of private payloads and omit them from public/forked copies.

## Scope & Preconditions
- Applies only when a tree is linked to GitHub (`tree.githubRepoFullName != null`).
- Privacy processing (strip/merge) runs only for non-fork trees (`!tree.isForked`). Forked trees never receive private payloads.
- Offline/local-only trees behave unchanged (no stripping/merging).

## Terminology & Flags
- Private marker: person has an `EventFact` with `tag="_PRIV"` (`U.PRIVATE_TAG`).
- DTO for private payload: `PrivatePerson { personId, mediaList, eventFacts }` in `oauthLibGithub/src/main/java/com/familygem/utility/PrivatePerson.java`.
- Local private file: `files/{treeId}.private.json`.
- Remote private file: `tree-private.json` in private repo `{owner}/{repo}-private`.

## Roles & Ownership
- **Owner tree**: main repo author; can toggle privacy and sync private payloads.
- **Forked tree**: does not get private payloads. Private persons remain with `_PRIV` tag and stripped content; viewing/editing is blocked unless fork owner matches current user (`Diagram.isCurrentUserOwnerOfTree`).

## Data Model Rules
- `_PRIV` tag must persist on private persons even after reinsertion of payload.
- Stripped fields: all events/facts and media. Names/IDs remain to preserve graph structure.
- Private payload stores deep clones of events/media (`TreeSplitter.cloneEventFact`).

## Lifecycle
### Mark person private
1. UI switch (IndividuoEventi) available only for online, non-fork trees.
2. `U.setPrivate(gedcom, person)` clones events/media into `PrivatePerson`, clears them on person, and adds `_PRIV` tag.

### Unmark person
- `U.setNonPrivate(person)` removes `_PRIV` tag; caller must restore data via `U.setNotPrivate(person, privatePerson)` when a saved payload is available.

### Save (U.salvaJson)
1. Preconditions: `githubRepoFullName` present and `!isForked`.
2. Collect all `_PRIV` persons → clone via `U.setPrivate`, accumulate `List<PrivatePerson>`.
3. Write `tree.json` (stripped) locally.
4. Write `{treeId}.private.json` locally via `U.savePrivatePersons`.
5. Restore private data back into in-memory Gedcom.
6. Push `tree.json` to main repo. If private payload exists, push `tree-private.json` to `{repo}-private` (SaveTreeFileTask).

### Load (Alberi.leggiJson)
1. Read `tree.json`.
2. If owner tree (`githubRepoFullName` set and `!isForked`), load `{treeId}.private.json` and merge events/media back into matching persons, leaving `_PRIV` tag intact.

### Forking protections
- During fork (`Facciata.forkRepo`), if source `tree.json` contains `_PRIV`, user is warned that private data is inaccessible (`U.doesForkedRepoContainPrivatePerson`).
- Diagram/person card: block open/edit when `tree.isForked && U.isPrivate(person)` unless current user owns the fork.

## UI Touchpoints
- Individual events screen: shows a “Private” switch only for owner trees; toggles `_PRIV` tag and triggers setPrivate/setNonPrivate.
- Diagram card tap: honors fork/privacy gate; logs and blocks when forked + private + not owned.

## Storage Layout
- Local files: `{treeId}.json` (public), `{treeId}.private.json` (private payload), `{treeId}.repo` (repo metadata), `{treeId}.commit` (last commit info, optional).
- Remote files: `tree.json` in main repo; `tree-private.json` in `{owner}/{repo}-private`.

## Error Handling
- Save pipeline reports commit hash conflicts (409) and refreshes repo metadata; errors surfaced via callbacks/toasts.
- If private save/upload fails, public `tree.json` still writes locally; private payload may be missing until next successful save.

## Constraints & Edge Considerations
- Fork recipients cannot recover private payloads; they only see placeholder `_PRIV` individuals.
- Exports (GEDCOM/PDF) are not privacy-filtered by this pipeline—behavior TBD; current logic only strips for GitHub saves.
- Ownership detection relies on stored GitHub user file (`files/user`), comparing to `githubRepoFullName` owner.

## Code Reference Map
- Privacy helpers and tags: `app/src/main/java/app/familygem/U.java` (methods: `isPrivate`, `setPrivate`, `setNonPrivate`, `setNotPrivate`, `savePrivatePersons`, `getPrivatePersons`, `doesForkedRepoContainPrivatePerson`).
- Save pipeline: `U.salvaJson`; remote write: `oauthLibGithub/.../SaveTreeFileTask.java`.
- Load pipeline: `app/src/main/java/app/familygem/Alberi.java` (`leggiJson`).
- UI switch: `app/src/main/java/app/familygem/IndividuoEventi.java` (`showPrivateSwitch`).
- Fork gating: `app/src/main/java/app/familygem/Diagram.java` (card tap logic, ownership check); `app/src/main/java/app/familygem/Facciata.java` (fork warning).
- Cloning utilities: `app/src/main/java/app/familygem/TreeSplitter.java` (`cloneEventFact`).

## Open Questions
- Should fork recipients see a clearer placeholder/notice for private nodes beyond the `_PRIV` tag and blocked navigation?
- Should exports honor privacy (strip or warn) consistently with GitHub saves?
- Should forks owned by the same user be allowed to pull private payloads automatically?
