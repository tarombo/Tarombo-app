package app.familygem;
import android.content.Context;
import android.util.Log;

import org.folg.gedcom.model.*;

import java.util.*;

public class RelationshipUtils {
    private static RelationshipUtils instance;
    private final Gedcom gedcom;
    private final Map<String, Person> personMap;
    private final Context context;

    private RelationshipUtils(Gedcom gedcom, Context context) {
        this.gedcom = gedcom;
        this.context = context;
        this.personMap = new HashMap<>();
        for (Person p : gedcom.getPeople()) {
            personMap.put(p.getId(), p);
        }
    }

    public static void createInstance(Gedcom gedcom, Context context) {
        instance = new RelationshipUtils(gedcom, context);
    }

    public static RelationshipUtils getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GedcomUtils is not initialized. Call createInstance(Gedcom, Context) first.");
        }
        return instance;
    }

    public static class RelationshipResult {
        public boolean bloodRelated;
        public int generationsBetween;
        public String relationship;
        public String fromName;
        public String toName;
        public int genA;
        public int genB;

        @Override
        public String toString() {
            if (!bloodRelated) {
                return fromName + " and " + toName + " are not blood-related.";
            }
            
            // Calculate actual generation difference (accounting for same generation relationships like cousins)
            int generationDifference = Math.abs(genA - genB);
            String generationText;
            if (generationDifference == 0) {
                generationText = "(same generation)";
            } else if (generationDifference == 1) {
                generationText = "(1 generation apart)";
            } else {
                generationText = String.format("(%d generations apart)", generationDifference);
            }
            
            return String.format(
                    "%s is %s's %s %s",
                    fromName,
                    toName,
                    relationship.toLowerCase(),
                    generationText
            );
        }
    }

    public RelationshipResult getRelationship(String idA, String idB) {
        Person a = personMap.get(idA);
        Person b = personMap.get(idB);

        RelationshipResult result = new RelationshipResult();
        result.fromName = U.epiteto(a);
        result.toName = U.epiteto(b);

        if (a == null || b == null) {
            result.bloodRelated = false;
            return result;
        }

        Map<Person, Integer> ancestorsA = getAncestorMap(a);
        Map<Person, Integer> ancestorsB = getAncestorMap(b);

        Set<Person> commonAncestors = new HashSet<>(ancestorsA.keySet());
        commonAncestors.retainAll(ancestorsB.keySet());

        if (commonAncestors.isEmpty()) {
            result.bloodRelated = false;
            return result;
        }

        result.bloodRelated = true;

        // Find the closest common ancestor (shortest total path)
        int minDistance = Integer.MAX_VALUE;
        Person closestAncestor = null;

        for (Person anc : commonAncestors) {
            int dist = ancestorsA.get(anc) + ancestorsB.get(anc);
            if (dist < minDistance) {
                minDistance = dist;
                closestAncestor = anc;
            }
        }

        int genA = ancestorsA.get(closestAncestor);
        int genB = ancestorsB.get(closestAncestor);
        result.generationsBetween = genA + genB;
        result.genA = genA;
        result.genB = genB;

        result.relationship = determineRelationship(genA, genB);

        return result;
    }

    private Map<Person, Integer> getAncestorMap(Person root) {
        Map<Person, Integer> ancestorMap = new HashMap<>();
        Queue<Person> queue = new LinkedList<>();
        Queue<Integer> levels = new LinkedList<>();

        ancestorMap.put(root, 0);
        queue.add(root);
        levels.add(0);

        while (!queue.isEmpty()) {
            Person current = queue.poll();
            int level = levels.poll();

            List<Family> families = getFamiliesAsChild(current);
            for (Family family : families) {
                for (String parentId : getParentIds(family)) {
                    Person parent = personMap.get(parentId);
                    if (parent != null && !ancestorMap.containsKey(parent)) {
                        Log.d("relationship", "  -> found parent: " + U.epiteto(parent) + " at level " + (level + 1));
                        ancestorMap.put(parent, level + 1);
                        queue.add(parent);
                        levels.add(level + 1);
                    }
                }
            }
        }

        return ancestorMap;
    }

    public Map<Person, Integer> getDescendants(Person root) {
        Map<Person, Integer> descendantMap = new HashMap<>();
        Queue<Person> queue = new LinkedList<>();
        Queue<Integer> levels = new LinkedList<>();

        queue.add(root);
        levels.add(0);

        while (!queue.isEmpty()) {
            Person current = queue.poll();
            int level = levels.poll();

            for (Family family : gedcom.getFamilies()) {
                List<String> parentIds = getParentIds(family);
                if (parentIds.contains(current.getId())) {
                    for (String childId : getChildIds(family)) {
                        Person child = personMap.get(childId);
                        if (child != null && !descendantMap.containsKey(child)) {
                            descendantMap.put(child, level + 1);
                            queue.add(child);
                            levels.add(level + 1);
                        }
                    }
                }
            }
        }

        return descendantMap;
    }

    public List<Person> getSiblings(Person person) {
        Set<Person> siblings = new HashSet<>();
        List<Family> families = getFamiliesAsChild(person);

        for (Family family : families) {
            for (String childId : getChildIds(family)) {
                if (!childId.equals(person.getId())) {
                    Person sibling = personMap.get(childId);
                    if (sibling != null) siblings.add(sibling);
                }
            }
        }

        return new ArrayList<>(siblings);
    }

    private List<Family> getFamiliesAsChild(Person person) {
        List<Family> families = new ArrayList<>();
        for (Family fam : gedcom.getFamilies()) {
            List<String> children = getChildIds(fam);
            Log.d("relationship", "Checking if person " + person.getId() + " is child in family " + fam.getId());
            Log.d("relationship", "  -> child IDs: " + children);
            if (children.contains(person.getId())) {
                families.add(fam);
            }
        }
        return families;
    }

    private List<String> getParentIds(Family family) {
        List<String> parentIds = new ArrayList<>();
        if (family.getHusbandRefs() != null) {
            for (SpouseRef ref : family.getHusbandRefs()) {
                parentIds.add(ref.getRef());
            }
        }
        if (family.getWifeRefs() != null) {
            for (SpouseRef ref : family.getWifeRefs()) {
                parentIds.add(ref.getRef());
            }
        }
        return parentIds;
    }

    private List<String> getChildIds(Family family) {
        List<String> childIds = new ArrayList<>();
        if (family.getChildRefs() != null) {
            for (ChildRef ref : family.getChildRefs()) {
                childIds.add(ref.getRef());
            }
        }
        return childIds;
    }

    private String determineRelationship(int genA, int genB) {
        if (genA == 0 && genB > 0) {
            switch (genB) {
                case 1: return context.getString(R.string.rel_parent);
                case 2: return context.getString(R.string.rel_grandparent);
                case 3: return context.getString(R.string.rel_great_grandparent);
                case 4: return context.getString(R.string.rel_great_great_grandparent);
                default: return genB + "x " + context.getString(R.string.rel_great_grandparent);
            }
        } else if (genB == 0 && genA > 0) {
            switch (genA) {
                case 1: return context.getString(R.string.rel_child);
                case 2: return context.getString(R.string.rel_grandchild);
                case 3: return context.getString(R.string.rel_great_grandchild);
                case 4: return context.getString(R.string.rel_great_great_grandchild);
                default: return genA + "x " + context.getString(R.string.rel_great_grandchild);
            }
        } else if (genA == genB) {
            switch (genA) {
                case 1: return context.getString(R.string.rel_sibling);
                case 2: return context.getString(R.string.rel_first_cousin);
                case 3: return context.getString(R.string.rel_second_cousin);
                case 4: return context.getString(R.string.rel_third_cousin);
                default: return genA + context.getString(R.string.rel_third_cousin); // "nth Cousin"
            }
        } else if (genA > 0 && genB > 0) {
            // Handle cousins with different generations (e.g., "First Cousin Once Removed")
            int minGen = Math.min(genA, genB);
            int timesRemoved = Math.abs(genA - genB);
            
            String cousinType;
            if (minGen == 1) {
                cousinType = context.getString(R.string.rel_sibling);
            } else if (minGen == 2) {
                cousinType = context.getString(R.string.rel_first_cousin);
            } else if (minGen == 3) {
                cousinType = context.getString(R.string.rel_second_cousin);
            } else if (minGen == 4) {
                cousinType = context.getString(R.string.rel_third_cousin);
            } else {
                cousinType = minGen + context.getString(R.string.rel_third_cousin); // "nth Cousin"
            }
            
            String removed;
            if (timesRemoved == 1) {
                removed = context.getString(R.string.rel_once_removed);
            } else if (timesRemoved == 2) {
                removed = context.getString(R.string.rel_twice_removed);
            } else if (timesRemoved == 3) {
                removed = context.getString(R.string.rel_thrice_removed);
            } else {
                removed = timesRemoved + "x " + context.getString(R.string.rel_once_removed); // Generic "x Removed"
            }
            
            return cousinType + " " + removed;
        } else {
            return context.getString(R.string.rel_distant);
        }
    }
}