package app.familygem;

import android.util.Log;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

import java.util.*;

/**
 * Utility class to extract a minimal family tree showing only the genealogical path
 * between two people, including their common ancestors and the connecting relatives.
 */
public class KinshipPathExtractor {

    // Static field to store original gedcom for restoration
    public static Gedcom originalGedcom = null;
    
    /**
     * Restores the original gedcom if it was stored
     */
    public static void restoreOriginalGedcom() {
        if (originalGedcom != null) {
            Global.gc = originalGedcom;
            originalGedcom = null;
        }
    }

    /**
     * Creates a minimal GEDCOM containing only the genealogical path between two people
     */
    public static Gedcom extractPath(Gedcom originalGedcom, Person personA, Person personB) {
        // Find all people in the genealogical path
        Set<String> pathPeopleIds = findPathPeople(originalGedcom, personA, personB);
        
        // Create new minimal GEDCOM
        Gedcom minimalGedcom = new Gedcom();
        minimalGedcom.setHeader(AlberoNuovo.creaTestata("kinship_path"));
        minimalGedcom.createIndexes();
        
        // Add only the people in the path
        for (String personId : pathPeopleIds) {
            Person originalPerson = originalGedcom.getPerson(personId);
            if (originalPerson != null) {
                Person clonedPerson = TreeSplitter.clonePerson(originalPerson, originalGedcom);
                minimalGedcom.addPerson(clonedPerson);
            }
        }
        
        // Add only families that connect path people
        Set<String> relevantFamilyIds = new HashSet<>();
        for (Family family : originalGedcom.getFamilies()) {
            if (isRelevantFamily(family, pathPeopleIds)) {
                relevantFamilyIds.add(family.getId());
                Family clonedFamily = TreeSplitter.cloneFamily(family);
                minimalGedcom.addFamily(clonedFamily);
            }
        }
        
        // Clean up person references to only include relevant families
        for (Person person : minimalGedcom.getPeople()) {
            cleanPersonFamilyRefs(person, relevantFamilyIds);
        }
        
        minimalGedcom.createIndexes();
        return minimalGedcom;
    }
    
    /**
     * Find all people involved in the genealogical path between two people
     */
    private static Set<String> findPathPeople(Gedcom gedcom, Person personA, Person personB) {
        Set<String> pathPeople = new HashSet<>();
        
        // Add the two main people
        pathPeople.add(personA.getId());
        pathPeople.add(personB.getId());
        
        Log.d("KinshipPath", "=== Finding path between ===");
        Log.d("KinshipPath", "Person A: " + U.getPrincipalName(personA) + " (ID: " + personA.getId() + ")");
        Log.d("KinshipPath", "Person B: " + U.getPrincipalName(personB) + " (ID: " + personB.getId() + ")");
        
        // First try to find blood relationship path
        Map<Person, Integer> ancestorsA = getAncestorMap(personA, gedcom);
        Map<Person, Integer> ancestorsB = getAncestorMap(personB, gedcom);
        
        Log.d("KinshipPath", "Person A has " + ancestorsA.size() + " ancestors");
        Log.d("KinshipPath", "Person B has " + ancestorsB.size() + " ancestors");
        
        // Find common ancestors
        List<Person> commonAncestors = new ArrayList<>();
        for (Person ancestor : ancestorsA.keySet()) {
            if (ancestorsB.containsKey(ancestor)) {
                commonAncestors.add(ancestor);
            }
        }
        
        Log.d("KinshipPath", "Found " + commonAncestors.size() + " common ancestors");
        
        if (!commonAncestors.isEmpty()) {
            // Blood relationship path exists
            Log.d("KinshipPath", "Using blood relationship path");
            Person closestCommonAncestor = commonAncestors.get(0);
            int minDistance = ancestorsA.get(closestCommonAncestor) + ancestorsB.get(closestCommonAncestor);
            
            for (Person ancestor : commonAncestors) {
                int distance = ancestorsA.get(ancestor) + ancestorsB.get(ancestor);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestCommonAncestor = ancestor;
                }
            }
            
            // Add path from personA to common ancestor
            addPathToAncestor(personA, closestCommonAncestor, gedcom, pathPeople);
            
            // Add path from personB to common ancestor
            addPathToAncestor(personB, closestCommonAncestor, gedcom, pathPeople);
            
            // Add the common ancestor
            pathPeople.add(closestCommonAncestor.getId());
        } else {
            // No blood relationship - try to find marriage/in-law connection path
            Log.d("KinshipPath", "No blood relationship found, checking marriage connections");
            Set<String> marriagePathPeople = findMarriageConnectionPath(gedcom, personA, personB);
            Log.d("KinshipPath", "Marriage path found " + marriagePathPeople.size() + " additional people");
            pathPeople.addAll(marriagePathPeople);
        }
        
        Log.d("KinshipPath", "Total path people: " + pathPeople.size());
        for (String personId : pathPeople) {
            Person person = gedcom.getPerson(personId);
            if (person != null) {
                Log.d("KinshipPath", "  - " + U.getPrincipalName(person) + " (ID: " + personId + ")");
            }
        }
        
        return pathPeople;
    }
    
    /**
     * Find connection path through marriage relationships when no blood relationship exists
     */
    private static Set<String> findMarriageConnectionPath(Gedcom gedcom, Person personA, Person personB) {
        Set<String> pathPeople = new HashSet<>();
        
        Log.d("KinshipPath", "=== Finding connection path via tree traversal ===");
        
        // Use BFS to find any path between personA and personB through the tree structure
        List<Person> connectionPath = findConnectionPathBFS(gedcom, personA, personB);
        
        if (!connectionPath.isEmpty()) {
            Log.d("KinshipPath", "Found connection path with " + connectionPath.size() + " people:");
            for (Person person : connectionPath) {
                pathPeople.add(person.getId());
                Log.d("KinshipPath", "  - " + U.getPrincipalName(person));
            }
        } else {
            Log.d("KinshipPath", "No connection path found via tree traversal");
        }
        
        return pathPeople;
    }
    
    /**
     * Use BFS to find any path between two people through parent-child, spouse, and sibling relationships
     */
    private static List<Person> findConnectionPathBFS(Gedcom gedcom, Person startPerson, Person targetPerson) {
        Queue<List<Person>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        // Start with the initial person
        List<Person> initialPath = new ArrayList<>();
        initialPath.add(startPerson);
        queue.add(initialPath);
        visited.add(startPerson.getId());
        
        Log.d("KinshipPath", "Starting BFS from " + U.getPrincipalName(startPerson) + " to find " + U.getPrincipalName(targetPerson));
        
        while (!queue.isEmpty()) {
            List<Person> currentPath = queue.poll();
            Person currentPerson = currentPath.get(currentPath.size() - 1);
            
            Log.d("KinshipPath", "Exploring from " + U.getPrincipalName(currentPerson) + " (path length: " + currentPath.size() + ")");
            
            // Check if we reached the target
            if (currentPerson.getId().equals(targetPerson.getId())) {
                Log.d("KinshipPath", "Found target! Path length: " + currentPath.size());
                return currentPath;
            }
            
            // Don't search too deep to avoid infinite loops
            if (currentPath.size() >= 6) {
                continue;
            }
            
            // Explore all connected people
            Set<Person> connectedPeople = getAllConnectedPeople(gedcom, currentPerson);
            for (Person connectedPerson : connectedPeople) {
                if (!visited.contains(connectedPerson.getId())) {
                    List<Person> newPath = new ArrayList<>(currentPath);
                    newPath.add(connectedPerson);
                    queue.add(newPath);
                    visited.add(connectedPerson.getId());
                    Log.d("KinshipPath", "  Added to queue: " + U.getPrincipalName(connectedPerson));
                }
            }
        }
        
        Log.d("KinshipPath", "No path found in BFS");
        return new ArrayList<>(); // Empty path if no connection found
    }
    
    /**
     * Get all people directly connected to a person (parents, children, spouses, siblings)
     */
    private static Set<Person> getAllConnectedPeople(Gedcom gedcom, Person person) {
        Set<Person> connected = new HashSet<>();
        
        // Get parents
        for (Family parentFamily : person.getParentFamilies(gedcom)) {
            connected.addAll(parentFamily.getHusbands(gedcom));
            connected.addAll(parentFamily.getWives(gedcom));
            
            // Get siblings
            for (Person sibling : parentFamily.getChildren(gedcom)) {
                if (!sibling.getId().equals(person.getId())) {
                    connected.add(sibling);
                }
            }
        }
        
        // Get spouses and children
        for (Family spouseFamily : person.getSpouseFamilies(gedcom)) {
            // Get spouses
            connected.addAll(spouseFamily.getHusbands(gedcom));
            connected.addAll(spouseFamily.getWives(gedcom));
            
            // Get children
            connected.addAll(spouseFamily.getChildren(gedcom));
        }
        
        // Remove self from the set
        connected.removeIf(p -> p.getId().equals(person.getId()));
        
        return connected;
    }
    
    
    /**
     * Add all members of spouse's parent family to show complete family context
     */
    private static void addParentFamilyMembers(Gedcom gedcom, Person spouse, Set<String> pathPeople) {
        for (Family parentFamily : spouse.getParentFamilies(gedcom)) {
            // Add parents
            for (Person parent : parentFamily.getHusbands(gedcom)) {
                pathPeople.add(parent.getId());
            }
            for (Person parent : parentFamily.getWives(gedcom)) {
                pathPeople.add(parent.getId());
            }
            // Add siblings
            for (Person sibling : parentFamily.getChildren(gedcom)) {
                pathPeople.add(sibling.getId());
            }
        }
    }
    
    /**
     * Get ancestor map with generation levels
     */
    private static Map<Person, Integer> getAncestorMap(Person root, Gedcom gedcom) {
        Map<Person, Integer> ancestorMap = new HashMap<>();
        Queue<Person> queue = new LinkedList<>();
        Queue<Integer> levels = new LinkedList<>();
        
        ancestorMap.put(root, 0);
        queue.add(root);
        levels.add(0);
        
        while (!queue.isEmpty()) {
            Person current = queue.poll();
            int level = levels.poll();
            
            // Get parents
            for (Family family : current.getParentFamilies(gedcom)) {
                List<String> parentIds = getParentIds(family);
                for (String parentId : parentIds) {
                    Person parent = gedcom.getPerson(parentId);
                    if (parent != null && !ancestorMap.containsKey(parent)) {
                        ancestorMap.put(parent, level + 1);
                        queue.add(parent);
                        levels.add(level + 1);
                    }
                }
            }
        }
        
        return ancestorMap;
    }
    
    /**
     * Add all people in the path from descendant to ancestor
     */
    private static void addPathToAncestor(Person descendant, Person ancestor, Gedcom gedcom, Set<String> pathPeople) {
        Queue<List<Person>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        List<Person> initialPath = new ArrayList<>();
        initialPath.add(descendant);
        queue.add(initialPath);
        visited.add(descendant.getId());
        
        while (!queue.isEmpty()) {
            List<Person> currentPath = queue.poll();
            Person currentPerson = currentPath.get(currentPath.size() - 1);
            
            if (currentPerson.getId().equals(ancestor.getId())) {
                // Found path - add all people in it
                for (Person person : currentPath) {
                    pathPeople.add(person.getId());
                }
                return;
            }
            
            // Get parents
            for (Family family : currentPerson.getParentFamilies(gedcom)) {
                List<String> parentIds = getParentIds(family);
                for (String parentId : parentIds) {
                    Person parent = gedcom.getPerson(parentId);
                    if (parent != null && !visited.contains(parent.getId())) {
                        List<Person> newPath = new ArrayList<>(currentPath);
                        newPath.add(parent);
                        queue.add(newPath);
                        visited.add(parent.getId());
                    }
                }
            }
        }
    }
    
    /**
     * Get parent IDs from a family (same as in RelationshipUtils)
     */
    private static List<String> getParentIds(Family family) {
        List<String> parentIds = new ArrayList<>();
        if (family.getHusbandRefs() != null) {
            for (org.folg.gedcom.model.SpouseRef ref : family.getHusbandRefs()) {
                parentIds.add(ref.getRef());
            }
        }
        if (family.getWifeRefs() != null) {
            for (org.folg.gedcom.model.SpouseRef ref : family.getWifeRefs()) {
                parentIds.add(ref.getRef());
            }
        }
        return parentIds;
    }
    
    /**
     * Check if a family is relevant (connects people in the path)
     */
    private static boolean isRelevantFamily(Family family, Set<String> pathPeopleIds) {
        // Check if any spouse or child is in the path
        for (String parentId : getParentIds(family)) {
            if (pathPeopleIds.contains(parentId)) {
                return true;
            }
        }
        
        if (family.getChildRefs() != null) {
            for (org.folg.gedcom.model.ChildRef childRef : family.getChildRefs()) {
                if (pathPeopleIds.contains(childRef.getRef())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Clean person's family references to only include relevant families
     */
    private static void cleanPersonFamilyRefs(Person person, Set<String> relevantFamilyIds) {
        // Clean parent family refs
        if (person.getParentFamilyRefs() != null) {
            person.getParentFamilyRefs().removeIf(ref -> !relevantFamilyIds.contains(ref.getRef()));
        }
        
        // Clean spouse family refs
        if (person.getSpouseFamilyRefs() != null) {
            person.getSpouseFamilyRefs().removeIf(ref -> !relevantFamilyIds.contains(ref.getRef()));
        }
    }
}