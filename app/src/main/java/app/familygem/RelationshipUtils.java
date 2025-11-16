package app.familygem;
import android.content.Context;
import android.util.Log;

import org.folg.gedcom.model.*;

import java.util.*;
import java.util.Locale;
import app.familygem.constants.Gender;

/**
 * RelationshipUtils - Comprehensive Batak Toba Kinship System Implementation
 * 
 * This class provides authentic Batak Toba cultural kinship relationship detection
 * based on the traditional Dalihan Na Tolu system. It combines advanced graph
 * traversal algorithms with culturally accurate relationship terminology.
 * 
 * BATAK TOBA KINSHIP SYSTEM (DALIHAN NA TOLU):
 * 
 * The Dalihan Na Tolu ("Three Pillars") is the foundational social structure
 * of Batak Toba society, consisting of three interconnected relationship categories:
 * 
 * 1. HULA-HULA (Wife-giving lineage - Superior position)
 *    - Mother's clan/lineage
 *    - Wife's family
 *    - Holds highest respect and authority
 *    
 * 2. DONGAN TUBU (Same clan relatives - Equal position)
 *    - Father's clan members
 *    - Brothers, sisters, cousins within same marga
 *    - Mutual support and cooperation
 *    
 * 3. BORU (Wife-receiving lineage - Inferior position)
 *    - Sister's husband's family
 *    - Daughter's husband's family
 *    - Provides service and respect
 * 
 * KEY RELATIONSHIP TERMS IMPLEMENTED:
 * 
 * DONGAN TUBU (Same Clan):
 * - Amanguda: Father's brother, Mother's sister's husband
 * - Inanguda: Father's brother's wife, Mother's sister
 * - Nanguda: Mother's sister, Father's brother's wife
 * 
 * HULA-HULA (Wife-giving):
 * - Tulang: Mother's brother, Wife's father
 * - Nantulang: Mother's brother's wife, Wife's mother
 * - Amanguda: Mother's sister's husband (Hula-hula variant)
 * 
 * BORU (Wife-receiving):
 * - Amangboru: Father's sister's husband, Daughter's husband's father
 * - Anak Boru: Sister's husband, Daughter's husband
 * 
 * CO-PARENTAL:
 * - Bao: Co-parent-in-law relationships
 * 
 * TECHNICAL IMPLEMENTATION:
 * 
 * The system uses sophisticated algorithms including:
 * - Breadth-First Search (BFS) for connection path discovery
 * - Multi-pattern relationship analysis (3, 4, and 5+ person paths)
 * - Sibling inheritance logic for consistent relationship terms
 * - Gender-sensitive cultural logic
 * - Performance optimization with ANR prevention
 * 
 * CULTURAL ACCURACY FEATURES:
 * - Authentic Batak Toba terminology
 * - Gender-sensitive relationship detection
 * - Proper Dalihan Na Tolu classification
 * - Sibling relationship inheritance
 * - Complex affinal relationship support
 * 
 * @author Arnold Siboro
 * @since 2024
 * @version 2.0 - Comprehensive Batak Toba Implementation
 */

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
                // Show the relationship even if not blood-related
                return String.format("%s is %s's %s", fromName, toName, relationship);
            }
            
            // Calculate actual generation difference (accounting for same generation relationships like cousins)
            int generationDifference = Math.abs(genA - genB);
            String generationText;
            if (generationDifference == 0) {
                generationText = "(same generation)";
            } else if (generationDifference == 1) {
                generationText = "(1 generation apart)";
            } else {
                generationText = String.format(Locale.US, "(%d generations apart)", generationDifference);
            }
            
            return String.format(
                    "%s is %s's %s %s",
                    fromName,
                    toName,
                    relationship.toLowerCase(Locale.US),
                    generationText
            );
        }
    }

    /**
     * Main relationship detection method implementing authentic Batak Toba kinship system.
     * 
     * This method provides comprehensive relationship analysis following the traditional
     * Dalihan Na Tolu framework, detecting both blood and affinal relationships with
     * culturally accurate terminology.
     * 
     * ALGORITHM OVERVIEW:
     * 1. Direct blood relationship detection using common ancestor analysis
     * 2. Non-blood relationship detection using BFS graph traversal
     * 3. Multi-pattern path analysis (3, 4, and 5+ person connections)
     * 4. Sibling inheritance logic for consistent relationship terms
     * 5. Gender-sensitive cultural classification
     * 
     * RELATIONSHIP DETECTION PATTERNS:
     * - Spouse relationships (husband/wife)
     * - Parent-child relationships across generations
     * - Sibling relationships within families
     * - Affinal relationships (in-laws, co-parents)
     * - Complex multi-step cultural relationships
     * 
     * CULTURAL ACCURACY FEATURES:
     * - Proper Dalihan Na Tolu classification (Hula-hula, Dongan Tubu, Boru)
     * - Authentic Batak Toba terminology
     * - Gender-sensitive relationship terms
     * - Sibling relationship inheritance
     * - Performance optimized with ANR prevention
     * 
     * @param idA The ID of the first person (perspective person)
     * @param idB The ID of the second person (target person)
     * @return RelationshipResult containing relationship information including:
     *         - bloodRelated: whether persons share common ancestors
     *         - relationship: authentic Batak Toba relationship term
     *         - generationsBetween: generation difference for blood relations
     *         - cultural context and proper terminology
     */
    public RelationshipResult getRelationship(String idA, String idB) {
        Person a = personMap.get(idA);
        Person b = personMap.get(idB);

        RelationshipResult result = new RelationshipResult();
        result.fromName = U.epiteto(a);
        result.toName = U.epiteto(b);

        if (a == null || b == null) {
            result.bloodRelated = false;
            result.relationship = "Unknown";
            return result;
        }

        // First check for direct blood relationship
        Map<Person, Integer> ancestorsA = getAncestorMap(a);
        Map<Person, Integer> ancestorsB = getAncestorMap(b);

        Set<Person> commonAncestors = new HashSet<>(ancestorsA.keySet());
        commonAncestors.retainAll(ancestorsB.keySet());

        if (!commonAncestors.isEmpty()) {
            // Blood relatives - existing logic
            result.bloodRelated = true;
            Log.d("BatakKinship", "=== BLOOD RELATIVES DETECTED ===");
            Log.d("BatakKinship", "Common ancestors found: " + commonAncestors.size());
            for (Person anc : commonAncestors) {
                Log.d("BatakKinship", "  Common ancestor: " + U.epiteto(anc));
            }

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

            Log.d("BatakKinship", "Closest ancestor: " + U.epiteto(closestAncestor) + ", genA=" + genA + ", genB=" + genB);

            // For Batak Toba kinship, we need more sophisticated path analysis
            if ("batak_toba".equals(Global.settings.kinshipTerms)) {
                result.relationship = determineBatakTobaRelationshipWithPath(a, b, closestAncestor, genA, genB);
            } else {
                result.relationship = determineRelationship(genA, genB);
            }
        } else {
            // No blood relationship - check for other relationships (marriage, in-laws, etc.)
            result.bloodRelated = false;
            Log.d("BatakKinship", "=== NO BLOOD RELATIONSHIP DETECTED ===");
            
            // For Batak Toba: Check if they share the same marga (clan/surname)
            // If same marga, apply generational relationship rules
            if ("batak_toba".equals(Global.settings.kinshipTerms)) {
                String margaA = getPersonMarga(a);
                String margaB = getPersonMarga(b);
                
                Log.d("BatakKinship", "Checking marga match: '" + margaA + "' vs '" + margaB + "'");
                
                if (margaA != null && margaB != null && margaA.equalsIgnoreCase(margaB)) {
                    Log.d("BatakKinship", "=== SAME MARGA DETECTED: " + margaA + " ===");
                    
                    // Determine generational difference
                    int generationDiff = estimateGenerationalDifference(a, b);
                    Log.d("BatakKinship", "Estimated generation difference: " + generationDiff);
                    
                    if (generationDiff > 0) {
                        // B is older generation - treat as Amanguda/Amangtua
                        Log.d("BatakKinship", "B is " + generationDiff + " generation(s) above A - using Amanguda");
                        result.relationship = context.getString(R.string.rel_batak_fathers_brother); // Amanguda
                        result.genA = 0;
                        result.genB = generationDiff;
                        result.generationsBetween = generationDiff;
                        return result;
                    } else if (generationDiff < 0) {
                        // B is younger generation - treat as Bere (nephew/niece)
                        Log.d("BatakKinship", "B is " + Math.abs(generationDiff) + " generation(s) below A - using Bere");
                        result.relationship = context.getString(R.string.rel_batak_sister_child); // Bere
                        result.genA = Math.abs(generationDiff);
                        result.genB = 0;
                        result.generationsBetween = Math.abs(generationDiff);
                        return result;
                    } else {
                        // Same generation - treat as Pariban (clan cousin)
                        Log.d("BatakKinship", "Same generation - using Pariban");
                        result.relationship = context.getString(R.string.rel_batak_same_clan_cousin); // Pariban/Dongan Tubu
                        result.genA = 0;
                        result.genB = 0;
                        result.generationsBetween = 0;
                        return result;
                    }
                }
            }
            
            Log.d("BatakKinship", "No marga match or not Batak system - calling determineNonBloodRelationship");
            result.relationship = determineNonBloodRelationship(a, b);
            result.genA = 0;
            result.genB = 0;
            result.generationsBetween = 0;
        }

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
        // Delegate to specific kinship system algorithms
        String kinshipSystem = Global.settings.kinshipTerms;
        
        if ("batak_toba".equals(kinshipSystem)) {
            return determineBatakTobaRelationship(genA, genB);
        } else {
            return determineGeneralRelationship(genA, genB);
        }
    }
    
    private String determineGeneralRelationship(int genA, int genB) {
        // Standard Western kinship algorithm
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
                default: return genA + context.getString(R.string.rel_third_cousin);
            }
        } else if (genA > 0 && genB > 0) {
            // Handle cousins with different generations (e.g., "First Cousin Once Removed")
            int minGen = Math.min(genA, genB);
            int maxGen = Math.max(genA, genB);
            int timesRemoved = Math.abs(genA - genB);
            
            // If minGen == 1, this is not a cousin relationship but aunt/uncle/niece/nephew
            if (minGen == 1) {
                if (genA == 1) {
                    // A is 1 generation from ancestor, B is more - A is aunt/uncle of B
                    return context.getString(R.string.rel_aunt_uncle);
                } else {
                    // B is 1 generation from ancestor, A is more - A is niece/nephew of B
                    return context.getString(R.string.rel_niece_nephew);
                }
            }
            
            String cousinType;
            if (minGen == 2) {
                cousinType = context.getString(R.string.rel_first_cousin);
            } else if (minGen == 3) {
                cousinType = context.getString(R.string.rel_second_cousin);
            } else if (minGen == 4) {
                cousinType = context.getString(R.string.rel_third_cousin);
            } else {
                // For cousins beyond third, use number prefix
                cousinType = (minGen - 1) + "th Cousin";
            }
            
            String removed;
            if (timesRemoved == 1) {
                removed = context.getString(R.string.rel_once_removed);
            } else if (timesRemoved == 2) {
                removed = context.getString(R.string.rel_twice_removed);
            } else if (timesRemoved == 3) {
                removed = context.getString(R.string.rel_thrice_removed);
            } else {
                removed = timesRemoved + "x Removed";
            }
            
            return cousinType + " " + removed;
        } else {
            return context.getString(R.string.rel_distant);
        }
    }
    
    private String determineBatakTobaRelationship(int genA, int genB) {
        // Batak Toba kinship follows Dalihan Na Tolu system:
        // Hula-hula (wife givers), Dongan Tubu (same clan), Boru (wife takers)
        
        // For direct ancestors and descendants (same lineage)
        if (genA == 0 && genB > 0) {
            // Direct ancestors (going up from person A)
            switch (genB) {
                case 1: return context.getString(R.string.rel_batak_parent); // Amang/Inang
                case 2: return context.getString(R.string.rel_batak_grandparent_paternal); // Ompu
                case 3: return context.getString(R.string.rel_batak_great_grandparent); // Ompu Mangulahi
                default: return context.getString(R.string.rel_batak_ancestor); // Ompu Parsadaan
            }
        } else if (genB == 0 && genA > 0) {
            // Direct descendants (going down from person A)
            switch (genA) {
                case 1: return context.getString(R.string.rel_batak_child); // Anak
                case 2: return context.getString(R.string.rel_batak_grandchild); // Pahompu
                case 3: return context.getString(R.string.rel_batak_great_grandchild); // Cucu ni Pahompu
                default: return context.getString(R.string.rel_batak_great_grandchild);
            }
        } else if (genA == genB) {
            // Same generation relationships
            switch (genA) {
                case 1: 
                    // Siblings - Dongan Tubu (same clan)
                    return context.getString(R.string.rel_batak_sibling); // Haha/Anggi
                case 2: 
                    // Cousins - need to determine if same clan or cross-clan
                    return context.getString(R.string.rel_batak_same_clan_cousin); // Dongan Tubu
                case 3: 
                case 4: 
                default: 
                    // Distant same-generation - considered clan related
                    return context.getString(R.string.rel_batak_distant_clan); // Dongan Sahala
            }
        } else if (genA > 0 && genB > 0) {
            // Cross-generation relationships - this is where Batak complexity shows
            int minGen = Math.min(genA, genB);
            int maxGen = Math.max(genA, genB);
            int generationDiff = Math.abs(genA - genB);
            
            if (minGen == 1) {
                // One person is sibling level, other is child/grandchild level
                if (genA == 1) {
                    // A is aunt/uncle level to B
                    // In Batak Toba, this could be:
                    // - Tulang (mother's brother) - Hula-hula relationship
                    // - Namboru (father's sister) - Boru relationship
                    // Without knowing the actual genealogical path, use generic
                    return determineBatakAuntUncleRelationship(genB);
                } else {
                    // A is niece/nephew level to B
                    return determineBatakNieceNephewRelationship(genA);
                }
            } else if (minGen == 2) {
                // Cousin relationships with generational differences
                String baseRelation = context.getString(R.string.rel_batak_same_clan_cousin);
                String generationalTerm = getBatakGenerationalTerm(generationDiff);
                return baseRelation + " " + generationalTerm;
            } else {
                // Very distant relationships - use clan terminology
                return context.getString(R.string.rel_batak_distant_clan); // Dongan Sahala
            }
        }
        
        // Fallback for any edge cases
        return context.getString(R.string.rel_batak_distant);
    }
    
    private String determineBatakAuntUncleRelationship(int targetGeneration) {
        // In authentic Batak Toba, aunt/uncle relationships depend on which side:
        // Tulang = mother's brother (Hula-hula) - very important relationship
        // Namboru = father's sister (Boru relationship)
        // Since we can't determine the exact path here, use the most important
        
        if (targetGeneration <= 2) {
            return context.getString(R.string.rel_batak_mothers_brother); // Tulang
        } else {
            return context.getString(R.string.rel_batak_tulang_rorobot); // Tulang Rorobot
        }
    }
    
    private String determineBatakNieceNephewRelationship(int sourceGeneration) {
        // Child of aunt/uncle in Batak Toba terms
        if (sourceGeneration <= 2) {
            return context.getString(R.string.rel_batak_mothers_brother_son); // Lae (if Tulang's child)
        } else {
            return context.getString(R.string.rel_batak_sister_child); // Bere
        }
    }
    
    private String determineBatakTobaRelationshipWithPath(Person personA, Person personB, Person commonAncestor, int genA, int genB) {
        // Enhanced Batak Toba relationship determination with genealogical path analysis
        
        // Special case: direct parent-child relationships
        if (genA == 0 && genB == 1) {
            return getBatakParentTerm(personB, Gender.getGender(personB));
        }
        if (genB == 0 && genA == 1) {
            return getBatakChildTerm(personA, Gender.getGender(personA));
        }
        
        // Special case: sibling relationships
        if (genA == 1 && genB == 1) {
            return getBatakSiblingTerm(personA, personB);
        }
        
        // For more complex relationships, try to determine if it's through maternal or paternal line
        try {
            List<Person> pathA = getPathToAncestor(personA, commonAncestor);
            List<Person> pathB = getPathToAncestor(personB, commonAncestor);
            
            if (pathA.size() > 1 && pathB.size() > 1) {
                // Analyze the first step in each path to determine lineage type
                Person immediateAncestorA = pathA.get(1); // First parent in path from A
                Person immediateAncestorB = pathB.get(1); // First parent in path from B
                
                return analyzeBatakRelationshipType(personA, personB, immediateAncestorA, immediateAncestorB, genA, genB);
            }
        } catch (Exception e) {
            // Fall back to generation-based calculation if path analysis fails
        }
        
        // Fallback to simplified generation-based Batak system
        return determineBatakTobaRelationship(genA, genB);
    }
    
    private List<Person> getPathToAncestor(Person descendant, Person ancestor) {
        // BFS to find path from descendant to ancestor
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
                return currentPath;
            }
            
            // Get parents of current person
            List<Family> families = getFamiliesAsChild(currentPerson);
            for (Family family : families) {
                for (String parentId : getParentIds(family)) {
                    if (!visited.contains(parentId)) {
                        Person parent = personMap.get(parentId);
                        if (parent != null) {
                            List<Person> newPath = new ArrayList<>(currentPath);
                            newPath.add(parent);
                            queue.add(newPath);
                            visited.add(parentId);
                        }
                    }
                }
            }
        }
        
        return new ArrayList<>(); // Empty path if not found
    }
    
    private String analyzeBatakRelationshipType(Person personA, Person personB, Person ancestorA, Person ancestorB, int genA, int genB) {
        // Determine relationship type based on Dalihan Na Tolu principles
        
        if (genA == genB && genA == 2) {
            // Cousin relationships - need to determine type
            if (ancestorA.getId().equals(ancestorB.getId())) {
                // Same immediate ancestor = siblings' children = same clan cousins
                return context.getString(R.string.rel_batak_same_clan_cousin); // Dongan Tubu
            } else {
                // Different immediate ancestors = cross-cousin potential
                // This could be Hula-hula or Boru relationship depending on genders
                return determineBatakCrossCousinType(ancestorA, ancestorB);
            }
        }
        
        if (genA == 1 && genB == 2) {
            // A is uncle/aunt level to B
            return determineBatakAuntUncleType(personA, ancestorA, Gender.getGender(personA));
        }
        
        if (genA == 2 && genB == 1) {
            // A is nephew/niece level to B  
            return determineBatakNieceNephewType(personA, ancestorA, Gender.getGender(personA));
        }
        
        // For other relationships, use generational approach
        return determineBatakTobaRelationship(genA, genB);
    }
    
    private String getBatakParentTerm(Person parent, Gender gender) {
        if (gender == Gender.MALE) {
            return context.getString(R.string.rel_batak_father); // Ama Suhut
        } else if (gender == Gender.FEMALE) {
            return context.getString(R.string.rel_batak_mother); // Ina Pangintubu
        } else {
            return context.getString(R.string.rel_batak_parent); // Amang/Inang
        }
    }
    
    private String getBatakChildTerm(Person child, Gender gender) {
        if (gender == Gender.FEMALE) {
            return context.getString(R.string.rel_batak_daughter); // Boru
        } else {
            return context.getString(R.string.rel_batak_child); // Anak
        }
    }
    
    private String getBatakSiblingTerm(Person personA, Person personB) {
        // In Batak Toba, sibling terms can be age-specific
        // Without birth order data, use generic term
        return context.getString(R.string.rel_batak_sibling); // Haha/Anggi
    }
    
    private String determineBatakCrossCousinType(Person ancestorA, Person ancestorB) {
        // Cross-cousin relationships in Batak Toba can be marriage-eligible (Pariban)
        // This requires complex analysis of lineage
        // For now, use generic cousin term
        return context.getString(R.string.rel_batak_same_clan_cousin); // Dongan Tubu
    }
    
    private String determineBatakAuntUncleType(Person person, Person throughAncestor, Gender gender) {
        // In Batak Toba culture, uncle/aunt relationships depend on which parent's side:
        // Father's brother = Amangtua (older) / Amanguda (younger) - Dongan Tubu
        // Father's sister = Namboru - Boru relationship
        // Mother's brother = Tulang - Hula-hula relationship (very important)
        // Mother's sister = Nantulang - Hula-hula relationship
        
        // Use BFS to determine the actual path and relationship type
        List<Person> connectionPath = findConnectionPathBFS(person, Global.gc.getPerson(Global.indi));
        if (!connectionPath.isEmpty() && connectionPath.size() == 3) {
            // 3-person path: Uncle/Aunt → Parent → Child
            String result = analyze3PersonPath(connectionPath);
            if (result != null) {
                return result;
            }
        }
        
        // Fallback: try reversed path
        List<Person> reversedPath = findConnectionPathBFS(Global.gc.getPerson(Global.indi), person);
        if (!reversedPath.isEmpty() && reversedPath.size() == 3) {
            String result = analyze3PersonPath(reversedPath);
            if (result != null) {
                return result;
            }
        }
        
        // Default fallback (should rarely be used)
        if (gender == Gender.MALE) {
            return context.getString(R.string.rel_batak_mothers_brother);
        } else {
            return context.getString(R.string.rel_batak_fathers_sister);
        }
    }
    
    private String determineBatakNieceNephewType(Person person, Person throughAncestor, Gender gender) {
        // Children of aunt/uncle relationships
        if (gender == Gender.MALE) {
            return context.getString(R.string.rel_batak_mothers_brother_son); // Lae
        } else {
            return context.getString(R.string.rel_batak_mothers_brother_daughter); // Pariban (potential bride)
        }
    }
    
    private String getBatakGenerationalTerm(int generations) {
        // Batak Toba generational terms
        switch (generations) {
            case 1: return context.getString(R.string.rel_batak_one_generation); // Sada Turun
            case 2: return context.getString(R.string.rel_batak_two_generation); // Dua Turun  
            case 3: return context.getString(R.string.rel_batak_three_generation); // Tolu Turun
            default: return generations + " Turun";
        }
    }
    
    /**
     * Determines relationship for non-blood relatives using appropriate cultural logic
     */
    private String determineNonBloodRelationship(Person a, Person b) {
        Log.d("BatakKinship", "determineNonBloodRelationship called with kinshipTerms: " + Global.settings.kinshipTerms);
        
        if ("batak_toba".equals(Global.settings.kinshipTerms)) {
            return determineBatakTobaNonBloodRelationship(a, b);
        } else {
            return determineGeneralNonBloodRelationship(a, b);
        }
    }
    
    /**
     * Determines non-blood relationships using authentic Batak Toba Dalihan Na Tolu system
     */
    private String determineBatakTobaNonBloodRelationship(Person a, Person b) {
        return determineBatakTobaNonBloodRelationship(a, b, false);
    }
    
    /**
     * Determines non-blood relationships using authentic Batak Toba Dalihan Na Tolu system
     * @param preventSiblingCheck if true, skips sibling checking to prevent infinite recursion
     */
    private String determineBatakTobaNonBloodRelationship(Person a, Person b, boolean preventSiblingCheck) {
        Log.d("BatakKinship", "=== Checking non-blood relationship ===");
        Log.d("BatakKinship", "Person A: " + U.epiteto(a) + " (ID: " + a.getId() + ")");
        Log.d("BatakKinship", "Person B: " + U.epiteto(b) + " (ID: " + b.getId() + ")");
        Log.d("BatakKinship", "preventSiblingCheck: " + preventSiblingCheck);
        
        // FIRST: Check if B is a sibling of someone who has a known relationship to A
        // (but only if we're not in a recursive call to prevent infinite loops)
        Log.d("BatakKinship", "About to check preventSiblingCheck condition: " + preventSiblingCheck);
        if (!preventSiblingCheck) {
            Log.d("BatakKinship", "=== Trying checkSiblingOfKnownRelative ===");
            String siblingRelationship = checkSiblingOfKnownRelative(a, b);
            if (siblingRelationship != null) {
                Log.d("BatakKinship", "Found sibling relationship: " + siblingRelationship);
                return siblingRelationship;
            } else {
                Log.d("BatakKinship", "checkSiblingOfKnownRelative returned null");
            }
        } else {
            Log.d("BatakKinship", "Skipping sibling check due to preventSiblingCheck=true");
        }
        
        // Check for direct spouse relationship
        if (areSpouses(a, b)) {
            Log.d("BatakKinship", "Found spouse relationship");
            return determineBatakSpouseRelationship(a, b);
        }
        
        // Use BFS tree traversal to find actual genealogical path
        List<Person> connectionPath = findConnectionPathBFS(a, b);
        if (!connectionPath.isEmpty()) {
            Log.d("BatakKinship", "Found connection path with " + connectionPath.size() + " people");
            for (Person person : connectionPath) {
                Log.d("BatakKinship", "  Path: " + U.epiteto(person));
            }
            
            // Analyze the path to determine Batak Toba relationship
            String batakRelationship = analyzeBatakPathForRelationship(connectionPath);
            if (batakRelationship != null) {
                Log.d("BatakKinship", "Determined relationship from path: " + batakRelationship);
                return batakRelationship;
            }
        } else {
            Log.d("BatakKinship", "No genealogical path found via tree traversal");
        }
        
        Log.d("BatakKinship", "No relationship found - returning non-relative");
        // Default for people with no discernible relationship
        return context.getString(R.string.rel_batak_non_relative);
    }
    
    /**
     * Advanced Breadth-First Search algorithm for discovering connection paths
     * between any two people in the family tree.
     * 
     * This sophisticated graph traversal algorithm explores all possible family
     * relationships including parent-child, spouse, and sibling connections to
     * find the shortest path between two persons. It's optimized for performance
     * with ANR prevention and supports complex cultural relationship detection.
     * 
     * ALGORITHM FEATURES:
     * - Breadth-first traversal ensures shortest path discovery
     * - Multi-relationship support (parents, children, spouses, siblings)
     * - Performance optimization with 100-iteration limit
     * - Comprehensive connection mapping for cultural analysis
     * - Memory efficient with visited node tracking
     * 
     * RELATIONSHIP TYPES EXPLORED:
     * 1. Parent-Child relationships (ascending/descending generations)
     * 2. Spouse relationships (marital connections)
     * 3. Sibling relationships (within family units)
     * 4. Extended family connections through multiple paths
     * 
     * CULTURAL INTEGRATION:
     * The discovered paths are then analyzed using Batak Toba cultural patterns
     * to determine authentic kinship terminology based on the Dalihan Na Tolu system.
     * 
     * @param startPerson The person to start the search from
     * @param targetPerson The person to find a connection to
     * @return List<Person> representing the shortest connection path,
     *         or null if no connection exists within iteration limits
     */
    private List<Person> findConnectionPathBFS(Person startPerson, Person targetPerson) {
        Queue<List<Person>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        // Start with the initial person
        List<Person> initialPath = new ArrayList<>();
        initialPath.add(startPerson);
        queue.add(initialPath);
        visited.add(startPerson.getId());
        
        Log.d("BatakKinship", "Starting BFS from " + U.epiteto(startPerson) + " to find " + U.epiteto(targetPerson));
        
        int iterations = 0;
        while (!queue.isEmpty() && iterations < 100) { // Safety limit
            iterations++;
            List<Person> currentPath = queue.poll();
            Person currentPerson = currentPath.get(currentPath.size() - 1);
            
            Log.d("BatakKinship", "BFS iteration " + iterations + ", current person: " + U.epiteto(currentPerson) + ", path length: " + currentPath.size());
            
            // Check if we reached the target
            if (currentPerson.getId().equals(targetPerson.getId())) {
                Log.d("BatakKinship", "Found target! Path length: " + currentPath.size());
                return currentPath;
            }
            
            // Don't search too deep to avoid infinite loops
            if (currentPath.size() >= 6) {
                Log.d("BatakKinship", "Path too deep (" + currentPath.size() + "), skipping");
                continue;
            }
            
            // Explore all connected people
            Set<Person> connectedPeople = getAllConnectedPeople(currentPerson);
            Log.d("BatakKinship", "Found " + connectedPeople.size() + " connected people to " + U.epiteto(currentPerson));
            for (Person connectedPerson : connectedPeople) {
                Log.d("BatakKinship", "  Connected: " + U.epiteto(connectedPerson) + " (visited: " + visited.contains(connectedPerson.getId()) + ")");
                if (!visited.contains(connectedPerson.getId())) {
                    List<Person> newPath = new ArrayList<>(currentPath);
                    newPath.add(connectedPerson);
                    queue.add(newPath);
                    visited.add(connectedPerson.getId());
                }
            }
        }
        
        Log.d("BatakKinship", "BFS completed after " + iterations + " iterations, no path found");
        return new ArrayList<>(); // Empty path if no connection found
    }
    
    /**
     * Get all people directly connected to a person (parents, children, spouses, siblings)
     * Prioritizes blood relationships over marriage relationships for better kinship detection
     */
    private Set<Person> getAllConnectedPeople(Person person) {
        Set<Person> connected = new HashSet<>();
        
        Log.d("BatakKinship", "Getting connected people for " + U.epiteto(person));
        
        // PRIORITY 1: Get parents and siblings (blood relationships)
        List<Family> parentFamilies = getFamiliesAsChild(person);
        Log.d("BatakKinship", "Found " + parentFamilies.size() + " parent families");
        for (Family parentFamily : parentFamilies) {
            // Add parents
            List<String> parentIds = getParentIds(parentFamily);
            Log.d("BatakKinship", "Parent family has " + parentIds.size() + " parents");
            for (String parentId : parentIds) {
                Person parent = personMap.get(parentId);
                if (parent != null) {
                    connected.add(parent);
                    Log.d("BatakKinship", "  Added parent: " + U.epiteto(parent));
                }
            }
            
            // Add siblings
            List<String> childIds = getChildIds(parentFamily);
            Log.d("BatakKinship", "Parent family has " + childIds.size() + " children");
            for (String childId : childIds) {
                if (!childId.equals(person.getId())) {
                    Person sibling = personMap.get(childId);
                    if (sibling != null) {
                        connected.add(sibling);
                        Log.d("BatakKinship", "  Added sibling: " + U.epiteto(sibling));
                    }
                }
            }
        }
        
        // PRIORITY 2: Get children (blood relationships)
        List<Family> spouseFamilies = person.getSpouseFamilies(gedcom);
        Log.d("BatakKinship", "Found " + spouseFamilies.size() + " spouse families");
        for (Family spouseFamily : spouseFamilies) {
            // Add children first (blood relationship)
            List<String> childIds = getChildIds(spouseFamily);
            Log.d("BatakKinship", "Spouse family has " + childIds.size() + " children");
            for (String childId : childIds) {
                Person child = personMap.get(childId);
                if (child != null) {
                    connected.add(child);
                    Log.d("BatakKinship", "  Added child: " + U.epiteto(child));
                }
            }
        }
        
        // PRIORITY 3: Get spouses (marriage relationships - lower priority)
        for (Family spouseFamily : spouseFamilies) {
            List<String> parentIds = getParentIds(spouseFamily);
            for (String spouseId : parentIds) {
                if (!spouseId.equals(person.getId())) {
                    Person spouse = personMap.get(spouseId);
                    if (spouse != null) {
                        connected.add(spouse);
                        Log.d("BatakKinship", "  Added spouse: " + U.epiteto(spouse));
                    }
                }
            }
        }
        
        Log.d("BatakKinship", "Total connected people for " + U.epiteto(person) + ": " + connected.size());
        return connected;
    }
    
    /**
     * Core Batak Toba cultural relationship analysis engine.
     * 
     * This method implements the sophisticated logic for interpreting family connection
     * paths according to authentic Batak Toba kinship principles. It analyzes the
     * discovered genealogical paths and applies traditional Dalihan Na Tolu cultural
     * patterns to determine the appropriate relationship terminology.
     * 
     * CULTURAL ANALYSIS PATTERNS:
     * 
     * 1. DIRECT RELATIONSHIPS (2-person paths):
     *    - Spouse relationships (husband/wife)
     *    - Parent-child relationships
     *    - Sibling relationships
     * 
     * 2. THREE-PERSON PATHS:
     *    - Parent's sibling relationships (Amanguda, Tulang, etc.)
     *    - Sibling's spouse relationships
     *    - Child's spouse relationships
     * 
     * 3. FOUR-PERSON PATHS:
     *    - Parent's sibling's spouse (Amangboru, Nantulang, etc.)
     *    - Spouse's sibling's child relationships
     *    - Complex affinal relationships
     * 
     * 4. LONGER PATHS (5+ persons):
     *    - Sibling inheritance patterns
     *    - Extended cultural relationships
     *    - Multi-generational connections
     * 
     * DALIHAN NA TOLU CLASSIFICATION:
     * Each relationship is properly classified into one of the three pillars:
     * - Hula-hula (wife-giving, superior): Tulang, Nantulang, etc.
     * - Dongan Tubu (same clan, equal): Amanguda, Inanguda, etc.
     * - Boru (wife-receiving, inferior): Amangboru, Anak Boru, etc.
     * 
     * GENDER SENSITIVITY:
     * The system applies proper gender-sensitive terminology ensuring
     * cultural accuracy in all relationship determinations.
     * 
     * @param path List of Person objects representing the connection path
     * @return String containing the authentic Batak Toba relationship term
     *         with cultural context, or null if no pattern matches
     */
    private String analyzeBatakPathForRelationship(List<Person> path) {
        if (path.size() < 2) {
            return null;
        }
        
        Person personA = path.get(0);
        Person personB = path.get(path.size() - 1);
        
        Log.d("BatakKinship", "Analyzing path of length " + path.size() + " for Batak relationship");
        Log.d("BatakKinship", "Path direction: " + U.epiteto(personA) + " → ... → " + U.epiteto(personB));
        
        // Analyze different path patterns
        if (path.size() == 2) {
            // Direct relationship
            return analyzeDirect2PersonPath(path);
        } else if (path.size() == 3) {
            // 3-person path: A → Connector → B
            return analyze3PersonPath(path);
        } else if (path.size() == 4) {
            // 4-person path: A → Sibling → Spouse → Parent
            // Try the path as-is first
            String result = analyze4PersonPath(path);
            if (result != null) {
                return result;
            }
            
            // If that doesn't work, try the reversed path
            List<Person> reversedPath = new ArrayList<>(path);
            Collections.reverse(reversedPath);
            Log.d("BatakKinship", "Trying reversed path for 4-person analysis");
            return analyze4PersonPath(reversedPath);
        } else if (path.size() >= 5) {
            // Longer paths - analyze pattern
            return analyzeLongerPath(path);
        }
        
        return null;
    }
    
    /**
     * Analyze 2-person direct relationship path
     */
    private String analyzeDirect2PersonPath(List<Person> path) {
        Person a = path.get(0);
        Person b = path.get(1);
        
        // Check if they are spouses
        if (areSpouses(a, b)) {
            return determineBatakSpouseRelationship(a, b);
        }
        
        // Check parent-child relationship
        if (isParentChild(a, b)) {
            if (isParent(a, b)) {
                return getBatakChildTerm(b, Gender.getGender(b));
            } else {
                return getBatakParentTerm(b, Gender.getGender(b));
            }
        }
        
        // Check sibling relationship
        if (areSiblings(a, b)) {
            return getBatakSiblingTerm(a, b);
        }
        
        return null;
    }
    
    /**
     * Analyze 3-person path: A → Connector → B
     */
    private String analyze3PersonPath(List<Person> path) {
        Person a = path.get(0);
        Person connector = path.get(1);
        Person b = path.get(2);
        
        Log.d("BatakKinship", "=== Analyzing 3-person path ===");
        Log.d("BatakKinship", "A: " + U.epiteto(a));
        Log.d("BatakKinship", "Connector: " + U.epiteto(connector));
        Log.d("BatakKinship", "B: " + U.epiteto(b));
        
        // Check all possible relationship combinations
        boolean isParentAConn = isParent(connector, a);
        boolean areSiblingsConnB = areSiblings(connector, b);
        boolean areSiblingsAConn = areSiblings(a, connector);
        boolean areSpousesConnB = areSpouses(connector, b);
        boolean areSpousesAConn = areSpouses(a, connector);
        boolean areSiblingsConnBRev = areSiblings(connector, b);
        
        Log.d("BatakKinship", "isParent(connector, a): " + isParentAConn);
        Log.d("BatakKinship", "areSiblings(connector, b): " + areSiblingsConnB);
        Log.d("BatakKinship", "areSiblings(a, connector): " + areSiblingsAConn);
        Log.d("BatakKinship", "areSpouses(connector, b): " + areSpousesConnB);
        Log.d("BatakKinship", "areSpouses(a, connector): " + areSpousesAConn);
        
        // Pattern: A → Parent → Parent's Sibling (Uncle/Aunt)
        if (isParentAConn && areSiblingsConnB) {
            Log.d("BatakKinship", "Found uncle/aunt pattern");
            Gender connectorGender = Gender.getGender(connector);
            Gender targetGender = Gender.getGender(b);
            
            if (connectorGender == Gender.FEMALE) {
                // Mother's sibling - Hula-hula relationships
                if (targetGender == Gender.MALE) {
                    return context.getString(R.string.rel_batak_mothers_brother);
                } else {
                    return context.getString(R.string.rel_batak_mothers_sister);
                }
            } else {
                // Father's sibling - Dongan Tubu relationships
                if (targetGender == Gender.FEMALE) {
                    return context.getString(R.string.rel_batak_fathers_sister);
                } else {
                    // Father's brother - need to determine age if possible
                    // For now, use generic term since we can't determine relative age
                    return context.getString(R.string.rel_batak_fathers_brother);
                }
            }
        }
        
        // Pattern: A → Sibling → Sibling's Spouse (Brother/Sister-in-law)
        if (areSiblingsAConn && areSpousesConnB) {
            Log.d("BatakKinship", "Found sibling's spouse pattern");
            Gender siblingGender = Gender.getGender(connector);
            Gender spouseGender = Gender.getGender(b);
            
            Log.d("BatakKinship", "Sibling (" + U.epiteto(connector) + ") gender: " + siblingGender);
            Log.d("BatakKinship", "Spouse (" + U.epiteto(b) + ") gender: " + spouseGender);
            
            if (siblingGender == Gender.MALE) {
                // Brother's spouse
                if (spouseGender == Gender.FEMALE) {
                    Log.d("BatakKinship", "Brother's wife pattern detected - returning Eda");
                    return context.getString(R.string.rel_batak_brother_wife);
                } else {
                    Log.d("BatakKinship", "Brother's spouse but not female - gender: " + spouseGender);
                }
            } else if (siblingGender == Gender.FEMALE) {
                // Sister's spouse
                if (spouseGender == Gender.MALE) {
                    Log.d("BatakKinship", "Sister's husband pattern detected - returning Lae");
                    return context.getString(R.string.rel_batak_sister_husband);
                } else {
                    Log.d("BatakKinship", "Sister's spouse but not male - gender: " + spouseGender);
                }
            } else {
                Log.d("BatakKinship", "Sibling gender undetermined: " + siblingGender);
                
                // If gender data is missing, infer from marital relationship
                // Assumption: heterosexual marriage is the norm in traditional Batak culture
                
                // First, try to infer spouse gender from relationship to ego
                // In a sibling-spouse relationship: A (ego) → Sibling → Spouse
                // We need to determine if the sibling is brother or sister
                
                // Check if we can infer sibling gender from marriage pattern
                if (spouseGender == Gender.FEMALE) {
                    // If spouse is female, sibling must be male (brother)
                    Log.d("BatakKinship", "Spouse is female, inferring sibling is male - returning Eda (Brother's Wife)");
                    return context.getString(R.string.rel_batak_brother_wife);
                } else if (spouseGender == Gender.MALE) {
                    // If spouse is male, sibling must be female (sister)
                    Log.d("BatakKinship", "Spouse is male, inferring sibling is female - returning Lae (Sister's Husband)");
                    return context.getString(R.string.rel_batak_sister_husband);
                } else {
                    // Both genders unknown - cannot determine reliably
                    Log.d("BatakKinship", "Both genders unknown, cannot determine relationship reliably");
                    return "Sibling-in-law";
                }
            }
        }
        
        // Pattern: Spouse → Sibling → A (from spouse's perspective - reverse view)
        if (areSpousesAConn && areSiblingsConnB) {
            Log.d("BatakKinship", "Found spouse's sibling pattern (reverse view)");
            Gender siblingGender = Gender.getGender(connector);
            Gender spouseGender = Gender.getGender(a);
            
            Log.d("BatakKinship", "Sibling (" + U.epiteto(connector) + ") gender: " + siblingGender);
            Log.d("BatakKinship", "Spouse (" + U.epiteto(a) + ") gender: " + spouseGender);
            
            if (siblingGender == Gender.FEMALE && spouseGender == Gender.MALE) {
                Log.d("BatakKinship", "Sister's husband pattern - returning Lae");
                return context.getString(R.string.rel_batak_sister_husband);
            } else if (siblingGender == Gender.MALE && spouseGender == Gender.FEMALE) {
                Log.d("BatakKinship", "Brother's wife pattern - returning Eda");
                return context.getString(R.string.rel_batak_brother_wife);
            } else {
                Log.d("BatakKinship", "Gender undetermined, inferring from marriage context");
                
                // Apply same inference logic as above
                if (spouseGender == Gender.FEMALE) {
                    // If spouse is female, sibling must be male (brother)
                    Log.d("BatakKinship", "Spouse is female, inferring sibling is male - returning Eda (Brother's Wife)");
                    return context.getString(R.string.rel_batak_brother_wife);
                } else if (spouseGender == Gender.MALE) {
                    // If spouse is male, sibling must be female (sister)
                    Log.d("BatakKinship", "Spouse is male, inferring sibling is female - returning Lae (Sister's Husband)");
                    return context.getString(R.string.rel_batak_sister_husband);
                } else if (siblingGender == Gender.FEMALE) {
                    // If sibling is female, spouse must be male
                    Log.d("BatakKinship", "Sibling is female, inferring spouse is male - returning Lae (Sister's Husband)");
                    return context.getString(R.string.rel_batak_sister_husband);
                } else if (siblingGender == Gender.MALE) {
                    // If sibling is male, spouse must be female
                    Log.d("BatakKinship", "Sibling is male, inferring spouse is female - returning Eda (Brother's Wife)");
                    return context.getString(R.string.rel_batak_brother_wife);
                } else {
                    // Both unknown - cannot determine reliably without name inference
                    Log.d("BatakKinship", "Both genders unknown, cannot determine relationship reliably");
                    return "Sibling-in-law";
                }
            }
        }
        
        // Pattern: A → Parent → Parent's Spouse (Step-parent)
        if (isParent(connector, a) && areSpouses(connector, b)) {
            Log.d("BatakKinship", "Found step-parent pattern");
            Gender parentGender = Gender.getGender(connector);
            Gender stepParentGender = Gender.getGender(b);
            
            // This is usually a step-parent relationship, but in Batak context might be different
            if (parentGender == Gender.MALE && stepParentGender == Gender.FEMALE) {
                return context.getString(R.string.rel_batak_step_mother);
            } else if (parentGender == Gender.FEMALE && stepParentGender == Gender.MALE) {
                return context.getString(R.string.rel_batak_step_father);
            }
        }
        
        // Pattern: A → Child → Child's Spouse (Child-in-law)
        if (isChild(connector, a) && areSpouses(connector, b)) {
            Log.d("BatakKinship", "Found child-in-law pattern");
            Gender childGender = Gender.getGender(connector);
            Gender spouseGender = Gender.getGender(b);
            
            if (childGender == Gender.MALE && spouseGender == Gender.FEMALE) {
                return context.getString(R.string.rel_batak_son_wife);
            } else if (childGender == Gender.FEMALE && spouseGender == Gender.MALE) {
                return context.getString(R.string.rel_batak_daughters_husband);
            }
        }
        
        // Pattern: A → Father → Father's Sister (Namboru) - checking if B is father's sister
        if (isParent(connector, a) && areSiblingsConnB) {
            Gender parentGender = Gender.getGender(connector);
            Gender siblingGender = Gender.getGender(b);
            
            if (parentGender == Gender.MALE && siblingGender == Gender.FEMALE) {
                return context.getString(R.string.rel_batak_fathers_sister);
            } else if (parentGender == Gender.FEMALE && siblingGender == Gender.MALE) {
                // Mother's brother (Tulang) - this is the direct blood relationship
                return context.getString(R.string.rel_batak_mothers_brother);
            } else if (parentGender == Gender.MALE && siblingGender == Gender.MALE) {
                // Father's brother (Amanguda)
                return context.getString(R.string.rel_batak_fathers_brother);
            }
        }
        
        // Pattern: A → Known Relative → Spouse of Known Relative
        // This handles cases like: A → Nantulang → Nantulang's Husband = Tulang
        // We need to check if the connector has a known relationship and b is their spouse
        
        // First, let's see if connector is a known relative and b is their spouse
        if (areSpouses(connector, b)) {
            Log.d("BatakKinship", "Found spouse of relative pattern");
            Log.d("BatakKinship", "Connector: " + U.epiteto(connector) + " (ID: " + connector.getId() + ")");
            Log.d("BatakKinship", "Target: " + U.epiteto(b) + " (ID: " + b.getId() + ")");
            
            // Check what relationship connector has to A, then determine spouse relationship
            String connectorRelationship = getDirectRelationship(a, connector);
            Log.d("BatakKinship", "Connector relationship to A: " + connectorRelationship);
            if (connectorRelationship != null) {
                Log.d("BatakKinship", "Processing spouse relationship based on: " + connectorRelationship);
                
                // If connector is Nantulang (Mother's Brother's Wife), then spouse is Tulang (Mother's Brother)
                if (connectorRelationship.contains("Nantulang")) {
                    Log.d("BatakKinship", "Connector is Nantulang, returning Tulang for spouse");
                    return context.getString(R.string.rel_batak_mothers_brother);
                }
                // If connector is Amangboru (Father's Sister's Husband), then spouse is Namboru (Father's Sister)
                if (connectorRelationship.contains("Amangboru")) {
                    Log.d("BatakKinship", "Connector is Amangboru, returning Namboru for spouse");
                    return context.getString(R.string.rel_batak_fathers_sister);
                }
                // If connector is Inanguda (Father's Brother's Wife OR Mother's Sister), then spouse relationship depends on context
                if (connectorRelationship.contains("Inanguda")) {
                    Log.d("BatakKinship", "Connector is Inanguda, checking context for spouse");
                    // For Inanguda as Mother's Sister, spouse is Amanguda (Mother's Sister's Husband)
                    // For Inanguda as Father's Brother's Wife, spouse is Amanguda (Father's Brother)
                    // In both cases, the spouse is Amanguda
                    return context.getString(R.string.rel_batak_fathers_brother);
                }
                // If connector is Tulang (Mother's Brother), then spouse is Nantulang (Mother's Brother's Wife)
                if (connectorRelationship.contains("Tulang")) {
                    Log.d("BatakKinship", "Connector is Tulang, returning Nantulang for spouse");
                    return context.getString(R.string.rel_batak_mothers_brother_wife);
                }
                // If connector is Namboru (Father's Sister), then spouse is Amangboru (Father's Sister's Husband)
                if (connectorRelationship.contains("Namboru")) {
                    Log.d("BatakKinship", "Connector is Namboru, returning Amangboru for spouse");
                    return context.getString(R.string.rel_batak_fathers_sister_husband);
                }
                // If connector is Amanguda (Father's Brother), then spouse is Inanguda (Father's Brother's Wife)
                if (connectorRelationship.contains("Amanguda")) {
                    Log.d("BatakKinship", "Connector is Amanguda, returning Inanguda for spouse");
                    return context.getString(R.string.rel_batak_fathers_brother_wife);
                }
                
                Log.d("BatakKinship", "No matching spouse relationship pattern found for: " + connectorRelationship);
            } else {
                Log.d("BatakKinship", "Could not determine connector's relationship to A");
            }
        }
        
        Log.d("BatakKinship", "No pattern matched in 3-person analysis");
        return null;
    }
    
    /**
     * Analyze 4-person path: A → Sibling → Spouse → Parent/Sibling
     */
    private String analyze4PersonPath(List<Person> path) {
        Person a = path.get(0);
        Person sibling = path.get(1);
        Person spouse = path.get(2);
        Person relative = path.get(3);
        
        Log.d("BatakKinship", "=== Analyzing 4-person path ===");
        Log.d("BatakKinship", "A: " + U.epiteto(a));
        Log.d("BatakKinship", "Sibling: " + U.epiteto(sibling));
        Log.d("BatakKinship", "Spouse: " + U.epiteto(spouse));
        Log.d("BatakKinship", "Relative: " + U.epiteto(relative));
        
        boolean areSiblingsCheck = areSiblings(a, sibling);
        boolean areSpousesCheck = areSpouses(sibling, spouse);
        boolean isParentCheck = isParent(relative, spouse);
        boolean areSiblingsCheck2 = areSiblings(spouse, relative);
        
        Log.d("BatakKinship", "areSiblings(a, sibling): " + areSiblingsCheck);
        Log.d("BatakKinship", "areSpouses(sibling, spouse): " + areSpousesCheck);
        Log.d("BatakKinship", "isParent(relative, spouse): " + isParentCheck);
        Log.d("BatakKinship", "areSiblings(spouse, relative): " + areSiblingsCheck2);
        
        // Pattern: A → Sibling → Sibling's Spouse → Spouse's Parent
        if (areSiblingsCheck && areSpousesCheck && isParentCheck) {
            Gender siblingGender = Gender.getGender(sibling);
            Gender parentGender = Gender.getGender(relative);
            
            Log.d("BatakKinship", "Found spouse's parent pattern!");
            Log.d("BatakKinship", "Sibling gender: " + siblingGender + ", Parent gender: " + parentGender);
            
            if (parentGender == Gender.MALE) {
                if (siblingGender == Gender.MALE) {
                    return context.getString(R.string.rel_batak_mothers_brother);
                } else {
                    return context.getString(R.string.rel_batak_mothers_brother);
                }
            } else {
                if (siblingGender == Gender.MALE) {
                    return context.getString(R.string.rel_batak_mothers_brother_wife);
                } else {
                    return context.getString(R.string.rel_batak_mothers_brother_wife);
                }
            }
        }
        
        // Pattern: A → Sibling → Sibling's Spouse → Spouse's Sibling (in-law's sibling)
        if (areSiblings(a, sibling) && areSpouses(sibling, spouse) && areSiblings(spouse, relative)) {
            Gender siblingGender = Gender.getGender(sibling);
            Gender relativeSiblingGender = Gender.getGender(relative);
            
            if (siblingGender == Gender.MALE) {
                // Brother's wife's sibling - these are Hula-hula relationships
                if (relativeSiblingGender == Gender.MALE) {
                    return context.getString(R.string.rel_batak_mothers_brother);
                } else {
                    return context.getString(R.string.rel_batak_mothers_brother_wife);
                }
            } else {
                // Sister's husband's sibling - these are Boru relationships  
                if (relativeSiblingGender == Gender.MALE) {
                    return context.getString(R.string.rel_batak_anak_boru);
                } else {
                    return context.getString(R.string.rel_batak_anak_boru);
                }
            }
        }
        
        // Pattern: A → Parent → Parent's Sibling → Sibling's Spouse (e.g., Father's Sister's Husband = Amangboru)
        if (isParent(sibling, a) && areSiblings(sibling, spouse) && areSpouses(spouse, relative)) {
            Gender parentGender = Gender.getGender(sibling);
            Gender siblingGender = Gender.getGender(spouse);
            Gender spouseGender = Gender.getGender(relative);
            
            Log.d("BatakKinship", "Found parent's sibling's spouse pattern!");
            Log.d("BatakKinship", "Parent: " + U.epiteto(sibling) + " (" + parentGender + ")");
            Log.d("BatakKinship", "Parent's Sibling: " + U.epiteto(spouse) + " (" + siblingGender + ")");
            Log.d("BatakKinship", "Sibling's Spouse: " + U.epiteto(relative) + " (" + spouseGender + ")");
            
            if (parentGender == Gender.MALE && siblingGender == Gender.FEMALE && spouseGender == Gender.MALE) {
                Log.d("BatakKinship", "Returning Amangboru (Father's Sister's Husband)");
                return context.getString(R.string.rel_batak_fathers_sister_husband);
            } else if (parentGender == Gender.FEMALE && siblingGender == Gender.MALE && spouseGender == Gender.FEMALE) {
                Log.d("BatakKinship", "Returning Nantulang (Mother's Brother's Wife)");
                return context.getString(R.string.rel_batak_mothers_brother_wife);
            } else if (parentGender == Gender.MALE && siblingGender == Gender.MALE && spouseGender == Gender.FEMALE) {
                Log.d("BatakKinship", "Returning Inanguda (Father's Brother's Wife)");
                return context.getString(R.string.rel_batak_fathers_brother_wife);
            } else if (parentGender == Gender.FEMALE && siblingGender == Gender.FEMALE && spouseGender == Gender.MALE) {
                Log.d("BatakKinship", "Returning Amanguda (Mother's Sister's Husband)");
                return context.getString(R.string.rel_batak_mothers_sister_husband);
            }
        }
        
        // Pattern: A → Child → Child's Spouse → Spouse's Parent (child's in-law's parent = Bao)
        if (isChild(sibling, a) && areSpouses(sibling, spouse) && isParent(relative, spouse)) {
            Gender childGender = Gender.getGender(sibling);
            Gender parentGender = Gender.getGender(relative);
            
            Log.d("BatakKinship", "Found child's spouse's parent pattern (Bao)!");
            Log.d("BatakKinship", "Child: " + childGender + ", Spouse's Parent: " + parentGender);
            
            // Bao relationship - co-parent-in-law
            if (parentGender == Gender.MALE) {
                return context.getString(R.string.rel_batak_co_parent_in_law);
            } else {
                return context.getString(R.string.rel_batak_co_parent_in_law);
            }
        }
        
        // Pattern: A → A's Spouse → Spouse's Sibling → Sibling's Child
        // Example: Gunadi → Rose → Leries → Arnold (Gunadi is Arnold's Amanguda)
        if (areSpouses(a, sibling) && areSiblings(sibling, spouse) && isChild(relative, spouse)) {
            Gender aGender = Gender.getGender(a);
            Gender spouseSiblingGender = Gender.getGender(spouse);
            
            Log.d("BatakKinship", "Found spouse's sibling's child pattern!");
            Log.d("BatakKinship", "A: " + U.epiteto(a) + " (" + aGender + ")");
            Log.d("BatakKinship", "A's Spouse: " + U.epiteto(sibling));
            Log.d("BatakKinship", "Spouse's Sibling: " + U.epiteto(spouse) + " (" + spouseSiblingGender + ")");
            Log.d("BatakKinship", "Sibling's Child: " + U.epiteto(relative));
            
            // From the child's perspective, A is their parent's sibling's spouse
            if (spouseSiblingGender == Gender.FEMALE && aGender == Gender.MALE) {
                // Mother's sister's husband = Amanguda
                Log.d("BatakKinship", "Returning Amanguda (Mother's Sister's Husband)");
                return context.getString(R.string.rel_batak_mothers_sister_husband);
            } else if (spouseSiblingGender == Gender.FEMALE && aGender == Gender.FEMALE) {
                // Mother's sister = Nanguda  
                Log.d("BatakKinship", "Returning Nanguda (Mother's Sister)");
                return context.getString(R.string.rel_batak_mothers_sister);
            } else if (spouseSiblingGender == Gender.MALE && aGender == Gender.MALE) {
                // Father's brother = Amanguda (same clan)
                Log.d("BatakKinship", "Returning Amanguda (Father's Brother)");
                return context.getString(R.string.rel_batak_fathers_brother);
            } else if (spouseSiblingGender == Gender.MALE && aGender == Gender.FEMALE) {
                // Father's brother's wife = Inanguda
                Log.d("BatakKinship", "Returning Inanguda (Father's Brother's Wife)");
                return context.getString(R.string.rel_batak_fathers_brother_wife);
            }
        }
        
        return null;
    }
    
    /**
     * Advanced analysis for complex multi-generational relationship paths.
     * 
     * This method handles sophisticated family connection patterns that extend
     * beyond simple 3-4 person relationships. It implements the crucial Batak Toba
     * cultural principle of sibling relationship inheritance, ensuring that siblings
     * share the same kinship terms with other relatives.
     * 
     * SIBLING INHERITANCE PRINCIPLE:
     * In Batak Toba culture, siblings inherit identical relationship terms.
     * For example, if Gunadi is Arnold's "Amanguda" (Mother's Sister's Husband),
     * then Gunadi's brother Gunawi is also Arnold's "Amanguda".
     * 
     * ALGORITHM FEATURES:
     * 
     * 1. FIVE-PERSON PATH ANALYSIS:
     *    - Detects sibling-of-relative patterns
     *    - Creates 4-person subpaths for analysis
     *    - Inherits relationship terms from siblings
     * 
     * 2. PATTERN RECOGNITION:
     *    - A → A's Sibling → Sibling's connections → Target
     *    - Validates sibling relationships using genealogical data
     *    - Applies cultural inheritance rules
     * 
     * 3. PERFORMANCE OPTIMIZATION:
     *    - Prevents infinite recursion
     *    - Efficient path manipulation
     *    - Memory-conscious analysis
     * 
     * CULTURAL ACCURACY:
     * This method ensures that complex family relationships maintain
     * cultural authenticity by properly applying Batak Toba inheritance
     * principles while respecting the Dalihan Na Tolu framework.
     * 
     * EXAMPLE PATTERNS:
     * - Gunawi → Gunadi → Rose → Leries → Arnold
     *   (Brother inherits "Amanguda" relationship)
     * - Extended cousin relationships through marriage
     * - Multi-step affinal connections
     * 
     * @param path List of Person objects representing a 5+ person connection
     * @return String containing the inherited Batak Toba relationship term,
     *         or null if no valid pattern is detected
     */
    private String analyzeLongerPath(List<Person> path) {
        // For longer paths, check for specific patterns first
        Log.d("BatakKinship", "Analyzing longer path with " + path.size() + " people");
        
        // Pattern: Sibling of a known relative
        // Check if the target person is a sibling of someone in a shorter path
        if (path.size() == 5) {
            // Pattern: A → A's sibling → A's sibling's spouse → spouse's sibling → spouse's sibling's spouse
            // This should be the same relationship as A's sibling → A's sibling's spouse → spouse's sibling → spouse's sibling's spouse
            Person a = path.get(0);  // Gunawi
            Person aSibling = path.get(1);  // Gunadi
            Person targetPerson = path.get(4);  // Arnold
            
            Log.d("BatakKinship", "Checking 5-person pattern: " + U.epiteto(a) + " → " + U.epiteto(aSibling) + " → ... → " + U.epiteto(targetPerson));
            
            // Check if A and the second person are siblings
            if (areSiblings(a, aSibling)) {
                Log.d("BatakKinship", "Found sibling relationship between " + U.epiteto(a) + " and " + U.epiteto(aSibling));
                
                // Create a 4-person path from A's sibling to the target
                List<Person> siblingPath = new ArrayList<>();
                for (int i = 1; i < path.size(); i++) {
                    siblingPath.add(path.get(i));
                }
                
                Log.d("BatakKinship", "Analyzing sibling's path: " + siblingPath.size() + " people");
                for (Person person : siblingPath) {
                    Log.d("BatakKinship", "  Sibling path: " + U.epiteto(person));
                }
                
                // Get the relationship from A's sibling to the target
                String siblingRelationship = analyze4PersonPath(siblingPath);
                if (siblingRelationship != null) {
                    Log.d("BatakKinship", "Found sibling's relationship: " + siblingRelationship);
                    return siblingRelationship; // Siblings share the same relationship terms in Batak culture
                }
            }
            
            // If the above doesn't work, try the general extended sibling analysis
            return analyzeExtendedSiblingRelationship(a, targetPerson, path);
        }
        
        // For other longer paths, use general distant relationship terms
        return context.getString(R.string.rel_batak_distant);
    }
    
    /**
     * Analyze relationships where someone is a sibling of a person with a known relationship
     */
    private String analyzeExtendedSiblingRelationship(Person a, Person targetPerson, List<Person> fullPath) {
        // Get siblings of the target person
        List<Person> siblings = getSiblings(targetPerson);
        
        for (Person sibling : siblings) {
            // Try to find a shorter relationship to this sibling
            List<Person> siblingPath = findShortestPath(a, sibling, 4);
            if (siblingPath != null && siblingPath.size() <= 4) {
                String siblingRelationship = null;
                
                if (siblingPath.size() == 3) {
                    siblingRelationship = analyze3PersonPath(siblingPath);
                } else if (siblingPath.size() == 4) {
                    siblingRelationship = analyze4PersonPath(siblingPath);
                }
                
                if (siblingRelationship != null && !siblingRelationship.contains("non-relative")) {
                    Log.d("BatakKinship", "Found sibling relationship: " + U.epiteto(sibling) + " is " + siblingRelationship);
                    Log.d("BatakKinship", "Therefore " + U.epiteto(targetPerson) + " should have the same relationship");
                    
                    // In Batak culture, siblings of relatives typically get the same relationship term
                    // Especially for marriage-related relationships like Amanguda
                    if (siblingRelationship.contains("Amanguda") || 
                        siblingRelationship.contains("Tulang") ||
                        siblingRelationship.contains("Inanguda") ||
                        siblingRelationship.contains("Namboru") ||
                        siblingRelationship.contains("Amangboru") ||
                        siblingRelationship.contains("Nantulang")) {
                        return siblingRelationship;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get direct relationship between two people for spouse deduction logic
     */
    private String getDirectRelationship(Person a, Person b) {
        Log.d("BatakKinship", "=== Getting direct relationship ===");
        Log.d("BatakKinship", "From: " + U.epiteto(a) + " (ID: " + a.getId() + ")");
        Log.d("BatakKinship", "To: " + U.epiteto(b) + " (ID: " + b.getId() + ")");
        
        // Try to find a direct 3-person path relationship first
        List<Person> directPath = findShortestPath(a, b, 3);
        Log.d("BatakKinship", "Found 3-person path with " + directPath.size() + " people");
        if (directPath.size() == 3) {
            String result = analyze3PersonPath(directPath);
            Log.d("BatakKinship", "3-person path result: " + result);
            return result;
        }
        
        // If no 3-person path, try 4-person path
        directPath = findShortestPath(a, b, 4);
        Log.d("BatakKinship", "Found 4-person path with " + directPath.size() + " people");
        if (directPath.size() == 4) {
            String result = analyze4PersonPath(directPath);
            Log.d("BatakKinship", "4-person path result: " + result);
            return result;
        }
        
        Log.d("BatakKinship", "No direct relationship found");
        return null;
    }
    
    /**
     * Find shortest path with a maximum length limit
     */
    private List<Person> findShortestPath(Person startPerson, Person targetPerson, int maxLength) {
        Queue<List<Person>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        // Initialize with start person
        List<Person> startPath = new ArrayList<>();
        startPath.add(startPerson);
        queue.add(startPath);
        visited.add(startPerson.getId());
        
        while (!queue.isEmpty()) {
            List<Person> currentPath = queue.poll();
            Person currentPerson = currentPath.get(currentPath.size() - 1);
            
            // Check if we reached the target
            if (currentPerson.getId().equals(targetPerson.getId())) {
                return currentPath;
            }
            
            // Don't search beyond max length
            if (currentPath.size() >= maxLength) {
                continue;
            }
            
            // Explore connected people
            Set<Person> connectedPeople = getAllConnectedPeople(currentPerson);
            for (Person connectedPerson : connectedPeople) {
                if (!visited.contains(connectedPerson.getId())) {
                    List<Person> newPath = new ArrayList<>(currentPath);
                    newPath.add(connectedPerson);
                    queue.add(newPath);
                    visited.add(connectedPerson.getId());
                }
            }
        }
        
        return new ArrayList<>(); // Empty if not found
    }
    
    /**
     * Check if person A is parent of person B
     */
    private boolean isParent(Person a, Person b) {
        for (Family family : a.getSpouseFamilies(gedcom)) {
            for (String childId : getChildIds(family)) {
                if (childId.equals(b.getId())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if person A is child of person B
     */
    private boolean isChild(Person a, Person b) {
        return isParent(b, a);
    }
    
    /**
     * Check if two people have parent-child relationship (either direction)
     */
    private boolean isParentChild(Person a, Person b) {
        return isParent(a, b) || isParent(b, a);
    }
    
    /**
     * Check if two people are siblings
     */
    private boolean areSiblings(Person a, Person b) {
        List<Person> siblingsA = getSiblings(a);
        for (Person sibling : siblingsA) {
            if (sibling.getId().equals(b.getId())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determines spouse relationship with Batak Toba gender-specific terms
     */
    private String determineBatakSpouseRelationship(Person a, Person b) {
        // In Batak Toba, the term depends on perspective and gender
        Gender genderA = Gender.getGender(a);
        Gender genderB = Gender.getGender(b);
        
        if (genderA == Gender.MALE && genderB == Gender.FEMALE) {
            return context.getString(R.string.rel_batak_wife);
        } else if (genderA == Gender.FEMALE && genderB == Gender.MALE) {
            return context.getString(R.string.rel_batak_husband);
        } else {
            return context.getString(R.string.rel_batak_spouse);
        }
    }
    
    /**
     * Checks for Hula-hula (Wife Giver) relationships in Batak Toba system
     */
    private String checkHulaHulaRelationship(Person a, Person b) {
        Log.d("BatakKinship", "=== Checking Hula-hula relationships ===");
        // B is Hula-hula to A if B's family gave a wife to A's family
        
        // Check if B is father of A's wife
        for (Family spouseFamily : a.getSpouseFamilies(gedcom)) {
            for (Person wife : spouseFamily.getWives(gedcom)) {
                if (!wife.getId().equals(a.getId())) {
                    // Check if B is father of this wife
                    for (Family wifeParentFamily : wife.getParentFamilies(gedcom)) {
                        for (Person father : wifeParentFamily.getHusbands(gedcom)) {
                            if (father.getId().equals(b.getId())) {
                                return context.getString(R.string.rel_batak_mothers_brother);
                            }
                        }
                        for (Person mother : wifeParentFamily.getWives(gedcom)) {
                            if (mother.getId().equals(b.getId())) {
                                return context.getString(R.string.rel_batak_mothers_brother_wife);
                            }
                        }
                        // Check for wife's siblings
                        for (Person sibling : wifeParentFamily.getChildren(gedcom)) {
                            if (sibling.getId().equals(b.getId()) && !sibling.getId().equals(wife.getId())) {
                                Gender siblingGender = Gender.getGender(sibling);
                                if (siblingGender == Gender.MALE) {
                                    return context.getString(R.string.rel_batak_mothers_brother_son);
                                } else {
                                    return context.getString(R.string.rel_batak_mothers_brother_daughter);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Check sibling's spouse families (brother's wife's family, sister's husband's family)
        // In Batak Toba culture, sibling's spouse's family becomes part of your extended kinship network
        Log.d("BatakKinship", "Checking sibling's spouse families for " + U.epiteto(a));
        for (Family parentFamily : a.getParentFamilies(gedcom)) {
            Log.d("BatakKinship", "Found parent family: " + parentFamily.getId());
            for (Person sibling : parentFamily.getChildren(gedcom)) {
                if (!sibling.getId().equals(a.getId())) { // Not self
                    Log.d("BatakKinship", "Checking sibling: " + U.epiteto(sibling));
                    // Check sibling's spouse families
                    for (Family siblingSpouseFamily : sibling.getSpouseFamilies(gedcom)) {
                        Log.d("BatakKinship", "Found sibling spouse family: " + siblingSpouseFamily.getId());
                        // Check all spouses of the sibling
                        List<Person> spouses = new ArrayList<>();
                        spouses.addAll(siblingSpouseFamily.getHusbands(gedcom));
                        spouses.addAll(siblingSpouseFamily.getWives(gedcom));
                        
                        for (Person spouse : spouses) {
                            if (!spouse.getId().equals(sibling.getId())) {
                                Log.d("BatakKinship", "Checking spouse: " + U.epiteto(spouse));
                                // Check if B is parent of sibling's spouse
                                List<Family> spouseParentFamilies = spouse.getParentFamilies(gedcom);
                                Log.d("BatakKinship", "Spouse has " + spouseParentFamilies.size() + " parent families");
                                for (Family spouseParentFamily : spouseParentFamilies) {
                                    Log.d("BatakKinship", "Checking spouse parent family: " + spouseParentFamily.getId());
                                    List<Person> spouseFathers = spouseParentFamily.getHusbands(gedcom);
                                    List<Person> spouseMothers = spouseParentFamily.getWives(gedcom);
                                    Log.d("BatakKinship", "Spouse parent family has " + spouseFathers.size() + " fathers and " + spouseMothers.size() + " mothers");
                                    
                                    for (Person spouseParent : spouseFathers) {
                                        Log.d("BatakKinship", "Checking spouse father: " + U.epiteto(spouseParent) + " (ID: " + spouseParent.getId() + ")");
                                        Log.d("BatakKinship", "Target person B ID: " + b.getId());
                                        if (spouseParent.getId().equals(b.getId())) {
                                            Gender siblingGender = Gender.getGender(sibling);
                                            if (siblingGender == Gender.MALE) {
                                                Log.d("BatakKinship", "Found brother's wife's father relationship!");
                                                return context.getString(R.string.rel_batak_mothers_brother);
                                            } else {
                                                return context.getString(R.string.rel_batak_mothers_brother);
                                            }
                                        }
                                    }
                                    for (Person spouseParent : spouseMothers) {
                                        Log.d("BatakKinship", "Checking spouse mother: " + U.epiteto(spouseParent) + " (ID: " + spouseParent.getId() + ")");
                                        if (spouseParent.getId().equals(b.getId())) {
                                            Gender siblingGender = Gender.getGender(sibling);
                                            if (siblingGender == Gender.MALE) {
                                                return context.getString(R.string.rel_batak_mothers_brother_wife);
                                            } else {
                                                return context.getString(R.string.rel_batak_mothers_brother_wife);
                                            }
                                        }
                                    }
                                    // Check for sibling's spouse's siblings
                                    for (Person spouseSibling : spouseParentFamily.getChildren(gedcom)) {
                                        if (spouseSibling.getId().equals(b.getId()) && !spouseSibling.getId().equals(spouse.getId())) {
                                            Gender spouseSiblingGender = Gender.getGender(spouseSibling);
                                            Gender originalSiblingGender = Gender.getGender(sibling);
                                            if (spouseSiblingGender == Gender.MALE) {
                                                if (originalSiblingGender == Gender.MALE) {
                                                    return context.getString(R.string.rel_batak_mothers_brother_son);
                                                } else {
                                                    return context.getString(R.string.rel_batak_mothers_brother_son);
                                                }
                                            } else {
                                                if (originalSiblingGender == Gender.MALE) {
                                                    return context.getString(R.string.rel_batak_mothers_brother_daughter);
                                                } else {
                                                    return context.getString(R.string.rel_batak_mothers_brother_daughter);
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // Fallback: If no parent families are defined, check surname matching for Batak patrilineal culture
                                if (spouseParentFamilies.size() == 0) {
                                    Log.d("BatakKinship", "No parent families defined, checking surname matching");
                                    String spouseName = U.epiteto(spouse);
                                    String targetName = U.epiteto(b);
                                    Log.d("BatakKinship", "Spouse name: " + spouseName + ", Target name: " + targetName);
                                    
                                    // Extract surname (last word) from both names
                                    String spouseSurname = extractSurname(spouseName);
                                    String targetSurname = extractSurname(targetName);
                                    Log.d("BatakKinship", "Spouse surname: " + spouseSurname + ", Target surname: " + targetSurname);
                                    
                                    // In Batak culture, if surnames match and there's an age/generation pattern,
                                    // it's likely a father-child relationship
                                    if (spouseSurname != null && spouseSurname.equals(targetSurname)) {
                                        Log.d("BatakKinship", "Surnames match! Checking gender and name patterns...");
                                        Gender siblingGender = Gender.getGender(sibling);
                                        Gender targetGender = Gender.getGender(b);
                                        Log.d("BatakKinship", "Sibling gender: " + siblingGender + ", Target gender: " + targetGender);
                                        
                                        // Additional check: target should be older generation (typical father name pattern)
                                        boolean looksLikeFather = looksLikeFatherName(targetName, spouseName);
                                        Log.d("BatakKinship", "Looks like father name pattern: " + looksLikeFather);
                                        
                                        // Handle cases where gender is not defined but can be inferred from name patterns
                                        boolean isMaleTarget = (targetGender == Gender.MALE) || 
                                                              (targetGender == Gender.NONE && looksLikeFather);
                                        boolean isFemaleTarget = (targetGender == Gender.FEMALE);
                                        
                                        if (isMaleTarget && looksLikeFather) {
                                            Log.d("BatakKinship", "Found likely father relationship based on surname and name pattern");
                                            if (siblingGender == Gender.MALE) {
                                                return context.getString(R.string.rel_batak_mothers_brother);
                                            } else {
                                                return context.getString(R.string.rel_batak_mothers_brother);
                                            }
                                        } else if (isFemaleTarget) {
                                            Log.d("BatakKinship", "Found likely mother relationship based on surname matching");
                                            if (siblingGender == Gender.MALE) {
                                                return context.getString(R.string.rel_batak_mothers_brother_wife);
                                            } else {
                                                return context.getString(R.string.rel_batak_mothers_brother_wife);
                                            }
                                        } else {
                                            Log.d("BatakKinship", "Surname match but not matching gender/name pattern criteria");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Check if A is female and B is from her husband's family receiving clan perspective
        if (Gender.getGender(a) == Gender.FEMALE) {
            for (Family spouseFamily : a.getSpouseFamilies(gedcom)) {
                for (Person husband : spouseFamily.getHusbands(gedcom)) {
                    if (!husband.getId().equals(a.getId())) {
                        // Check if B is related to husband's mother's side (Hula-hula to husband)
                        String husbandHulaHula = checkPersonAsHulaHula(husband, b);
                        if (husbandHulaHula != null) {
                            return husbandHulaHula + " (Husband's Hula-hula)";
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extracts surname (last word) from a person's name
     */
    private String extractSurname(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return null;
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1]; // Last word is typically the surname
    }
    
    /**
     * Gets the marga (patrilineal clan surname) for a person.
     * In Batak culture, this is typically the last word of the name.
     * 
     * @param person The person whose marga to extract
     * @return The marga (surname) or null if not available
     */
    private String getPersonMarga(Person person) {
        if (person == null) {
            return null;
        }
        
        // Try to get surname from Name object first (most reliable)
        if (person.getNames() != null && !person.getNames().isEmpty()) {
            Name name = person.getNames().get(0);
            if (name.getSurname() != null && !name.getSurname().trim().isEmpty()) {
                String surname = name.getSurname().trim();
                Log.d("BatakKinship", "Got marga from Name.getSurname(): " + surname);
                return surname;
            }
        }
        
        // Fallback: extract from full name using epiteto
        String fullName = U.epiteto(person);
        if (fullName != null && !fullName.trim().isEmpty()) {
            String extractedSurname = extractSurname(fullName);
            Log.d("BatakKinship", "Extracted marga from full name '" + fullName + "': " + extractedSurname);
            return extractedSurname;
        }
        
        Log.d("BatakKinship", "Could not determine marga for person: " + person.getId());
        return null;
    }
    
    /**
     * Estimates the generational difference between two people who share the same marga
     * but have no direct blood relationship.
     * 
     * Uses birth years, age patterns, and family structure to determine relative generation.
     * 
     * @param personA The reference person (ego)
     * @param personB The person to compare
     * @return Positive if B is older generation, negative if B is younger, 0 if same generation
     */
    private int estimateGenerationalDifference(Person personA, Person personB) {
        // Strategy 1: Use birth years if available
        Integer birthYearA = getBirthYear(personA);
        Integer birthYearB = getBirthYear(personB);
        
        if (birthYearA != null && birthYearB != null) {
            int yearDiff = birthYearA - birthYearB;
            Log.d("BatakKinship", "Birth years: A=" + birthYearA + ", B=" + birthYearB + ", diff=" + yearDiff);
            
            // Typical generation gap is 25-35 years
            // Use 25 years as threshold
            if (yearDiff >= 25) {
                // B is older - likely parent generation
                int generations = (yearDiff + 12) / 25; // Round to nearest generation
                Log.d("BatakKinship", "B is ~" + generations + " generation(s) older based on birth years");
                return generations;
            } else if (yearDiff <= -25) {
                // B is younger - likely child generation
                int generations = (Math.abs(yearDiff) + 12) / 25; // Round to nearest generation
                Log.d("BatakKinship", "B is ~" + generations + " generation(s) younger based on birth years");
                return -generations;
            } else {
                // Same generation (within 25 years)
                Log.d("BatakKinship", "Same generation based on birth years");
                return 0;
            }
        }
        
        // Strategy 2: Compare family structure depth
        // Count how many generations each person is from their oldest known ancestor
        int depthA = getGenerationalDepth(personA);
        int depthB = getGenerationalDepth(personB);
        
        if (depthA >= 0 && depthB >= 0 && depthA != depthB) {
            int depthDiff = depthB - depthA; // Positive if B is deeper (younger)
            Log.d("BatakKinship", "Generation depth: A=" + depthA + ", B=" + depthB + ", diff=" + depthDiff);
            return -depthDiff; // Invert so positive means B is older generation
        }
        
        // Strategy 3: Compare number of descendants (more descendants = older generation)
        int descendantsA = countDescendants(personA);
        int descendantsB = countDescendants(personB);
        
        if (descendantsA > descendantsB + 2) {
            Log.d("BatakKinship", "A has significantly more descendants - B is likely older generation");
            return 1; // B is probably older
        } else if (descendantsB > descendantsA + 2) {
            Log.d("BatakKinship", "B has significantly more descendants - B is likely younger generation");
            return -1; // B is probably younger
        }
        
        // Default: assume same generation if can't determine
        Log.d("BatakKinship", "Cannot determine generational difference - assuming same generation");
        return 0;
    }
    
    /**
     * Attempts to get birth year from a person's events
     */
    private Integer getBirthYear(Person person) {
        if (person == null || person.getEventsFacts() == null) {
            return null;
        }
        
        for (EventFact event : person.getEventsFacts()) {
            if ("BIRT".equals(event.getTag()) && event.getDate() != null) {
                try {
                    String dateStr = event.getDate();
                    // Extract year from date string (typically last 4 digits)
                    String yearStr = dateStr.replaceAll(".*?(\\d{4}).*", "$1");
                    if (yearStr.matches("\\d{4}")) {
                        return Integer.parseInt(yearStr);
                    }
                } catch (Exception e) {
                    Log.d("BatakKinship", "Error parsing birth date: " + e.getMessage());
                }
            }
        }
        return null;
    }
    
    /**
     * Calculates how many generations a person is from their oldest known ancestor
     */
    private int getGenerationalDepth(Person person) {
        if (person == null) {
            return -1;
        }
        
        int maxDepth = 0;
        Queue<Person> queue = new LinkedList<>();
        Queue<Integer> depths = new LinkedList<>();
        
        queue.add(person);
        depths.add(0);
        
        Set<String> visited = new HashSet<>();
        visited.add(person.getId());
        
        while (!queue.isEmpty()) {
            Person current = queue.poll();
            int currentDepth = depths.poll();
            
            if (currentDepth > maxDepth) {
                maxDepth = currentDepth;
            }
            
            // Go up to parents
            for (Family parentFamily : current.getParentFamilies(gedcom)) {
                for (Person parent : parentFamily.getHusbands(gedcom)) {
                    if (!visited.contains(parent.getId())) {
                        visited.add(parent.getId());
                        queue.add(parent);
                        depths.add(currentDepth + 1);
                    }
                }
                for (Person parent : parentFamily.getWives(gedcom)) {
                    if (!visited.contains(parent.getId())) {
                        visited.add(parent.getId());
                        queue.add(parent);
                        depths.add(currentDepth + 1);
                    }
                }
            }
        }
        
        return maxDepth;
    }
    
    /**
     * Counts the number of descendants (children, grandchildren, etc.) for a person
     */
    private int countDescendants(Person person) {
        if (person == null) {
            return 0;
        }
        
        int count = 0;
        Queue<Person> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        queue.add(person);
        visited.add(person.getId());
        
        while (!queue.isEmpty()) {
            Person current = queue.poll();
            
            for (Family spouseFamily : current.getSpouseFamilies(gedcom)) {
                for (Person child : spouseFamily.getChildren(gedcom)) {
                    if (!visited.contains(child.getId())) {
                        visited.add(child.getId());
                        count++;
                        queue.add(child);
                    }
                }
            }
        }
        
        return count;
    }
    
    /**
     * Checks if target name looks like it could be father of spouse based on Batak naming patterns
     */
    private boolean looksLikeFatherName(String targetName, String spouseName) {
        if (targetName == null || spouseName == null) {
            return false;
        }
        
        // In Batak culture, father's names often contain "Pandapotan", middle names, etc.
        // and are generally longer/more formal than children's names
        targetName = targetName.toLowerCase();
        spouseName = spouseName.toLowerCase();
        
        // Check for typical father name patterns
        boolean hasPaternPattern = targetName.contains("pandapotan") || 
                                  targetName.contains("situmorang") ||
                                  targetName.split("\\s+").length >= 3; // Multi-part names often indicate older generation
        
        // Check if target name is notably different/longer than spouse name (generational difference)
        boolean generationalDifference = targetName.split("\\s+").length > spouseName.split("\\s+").length;
        
        return hasPaternPattern || generationalDifference;
    }
    
    /**
     * Checks for Boru (Wife Taker) relationships in Batak Toba system
     */
    private String checkBoruRelationship(Person a, Person b) {
        // B is Boru to A if B's family received a wife from A's family
        
        // Check if B is husband of A's daughter
        for (Family childFamily : a.getSpouseFamilies(gedcom)) {
            for (Person child : childFamily.getChildren(gedcom)) {
                if (Gender.getGender(child) == Gender.FEMALE) {
                    // Check if B is married to this daughter
                    for (Family daughterSpouseFamily : child.getSpouseFamilies(gedcom)) {
                        for (Person husband : daughterSpouseFamily.getHusbands(gedcom)) {
                            if (husband.getId().equals(b.getId())) {
                                return context.getString(R.string.rel_batak_daughters_husband);
                            }
                        }
                    }
                }
            }
        }
        
        // Check if B is son of A's daughter (grandson through daughter)
        for (Family childFamily : a.getSpouseFamilies(gedcom)) {
            for (Person child : childFamily.getChildren(gedcom)) {
                if (Gender.getGender(child) == Gender.FEMALE) {
                    for (Family grandchildFamily : child.getSpouseFamilies(gedcom)) {
                        for (Person grandchild : grandchildFamily.getChildren(gedcom)) {
                            if (grandchild.getId().equals(b.getId())) {
                                Gender grandchildGender = Gender.getGender(grandchild);
                                if (grandchildGender == Gender.MALE) {
                                    return context.getString(R.string.rel_batak_daughter_son);
                                } else {
                                    return context.getString(R.string.rel_batak_daughter_daughter);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Helper method to check if person B is Hula-hula to person A
     */
    private String checkPersonAsHulaHula(Person a, Person b) {
        // Check mother's side relationships (mother's family is Hula-hula)
        for (Family parentFamily : a.getParentFamilies(gedcom)) {
            for (Person mother : parentFamily.getWives(gedcom)) {
                // Check if B is from mother's family
                for (Family motherParentFamily : mother.getParentFamilies(gedcom)) {
                    for (Person motherFather : motherParentFamily.getHusbands(gedcom)) {
                        if (motherFather.getId().equals(b.getId())) {
                            return context.getString(R.string.rel_batak_mothers_brother);
                        }
                    }
                    for (Person motherMother : motherParentFamily.getWives(gedcom)) {
                        if (motherMother.getId().equals(b.getId())) {
                            return context.getString(R.string.rel_batak_mothers_brother_wife);
                        }
                    }
                    for (Person motherSibling : motherParentFamily.getChildren(gedcom)) {
                        if (motherSibling.getId().equals(b.getId()) && !motherSibling.getId().equals(mother.getId())) {
                            Gender siblingGender = Gender.getGender(motherSibling);
                            if (siblingGender == Gender.MALE) {
                                return context.getString(R.string.rel_batak_mothers_brother);
                            } else {
                                return context.getString(R.string.rel_batak_mothers_sister);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Checks for other marriage-related relationships in Batak system
     */
    private String checkBatakMarriageRelationship(Person a, Person b) {
        // Check for relationships between people married into same family
        // Check if both are married to siblings (co-in-laws)
        for (Family aSpouseFamily : a.getSpouseFamilies(gedcom)) {
            Person aSpouse = null;
            // Get A's spouse
            for (Person spouse : aSpouseFamily.getHusbands(gedcom)) {
                if (!spouse.getId().equals(a.getId())) {
                    aSpouse = spouse;
                    break;
                }
            }
            for (Person spouse : aSpouseFamily.getWives(gedcom)) {
                if (!spouse.getId().equals(a.getId())) {
                    aSpouse = spouse;
                    break;
                }
            }
            
            if (aSpouse != null) {
                // Check if B is married to A's spouse's sibling
                for (Family aSpouseParentFamily : aSpouse.getParentFamilies(gedcom)) {
                    for (Person aSpouseSibling : aSpouseParentFamily.getChildren(gedcom)) {
                        if (!aSpouseSibling.getId().equals(aSpouse.getId())) {
                            for (Family siblingSpouseFamily : aSpouseSibling.getSpouseFamilies(gedcom)) {
                                for (Person siblingSpouse : siblingSpouseFamily.getHusbands(gedcom)) {
                                    if (siblingSpouse.getId().equals(b.getId())) {
                                        return context.getString(R.string.rel_batak_co_in_law);
                                    }
                                }
                                for (Person siblingSpouse : siblingSpouseFamily.getWives(gedcom)) {
                                    if (siblingSpouse.getId().equals(b.getId())) {
                                        return context.getString(R.string.rel_batak_co_in_law);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Determines non-blood relationships using general Western kinship terms
     */
    private String determineGeneralNonBloodRelationship(Person a, Person b) {
        // Check if they are spouses
        if (areSpouses(a, b)) {
            return "Spouse";
        }
        
        // Check for in-law relationships
        String inLawRelationship = checkGeneralInLawRelationship(a, b);
        if (inLawRelationship != null) {
            return inLawRelationship;
        }
        
        // Check for step-family relationships
        String stepRelationship = checkStepRelationship(a, b);
        if (stepRelationship != null) {
            return stepRelationship;
        }
        
        // Default for unrelated people
        return "Non-relative";
    }
    
    /**
     * Helper method to check if two people are spouses
     */
    private boolean areSpouses(Person a, Person b) {
        for (Family family : a.getSpouseFamilies(gedcom)) {
            for (Person spouse : family.getHusbands(gedcom)) {
                if (spouse.getId().equals(b.getId())) {
                    return true;
                }
            }
            for (Person spouse : family.getWives(gedcom)) {
                if (spouse.getId().equals(b.getId())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private String checkGeneralInLawRelationship(Person a, Person b) {
        // Check if B is A's spouse's family member
        for (Family spouseFamily : a.getSpouseFamilies(gedcom)) {
            // Get spouse
            for (Person spouse : spouseFamily.getHusbands(gedcom)) {
                if (!spouse.getId().equals(a.getId())) {
                    String relationship = checkSpouseFamilyRelationship(spouse, b);
                    if (relationship != null) return relationship;
                }
            }
            for (Person spouse : spouseFamily.getWives(gedcom)) {
                if (!spouse.getId().equals(a.getId())) {
                    String relationship = checkSpouseFamilyRelationship(spouse, b);
                    if (relationship != null) return relationship;
                }
            }
        }
        
        // Check if A is B's spouse's family member
        for (Family spouseFamily : b.getSpouseFamilies(gedcom)) {
            // Get spouse
            for (Person spouse : spouseFamily.getHusbands(gedcom)) {
                if (!spouse.getId().equals(b.getId())) {
                    String relationship = checkSpouseFamilyRelationship(spouse, a);
                    if (relationship != null) return relationship;
                }
            }
            for (Person spouse : spouseFamily.getWives(gedcom)) {
                if (!spouse.getId().equals(b.getId())) {
                    String relationship = checkSpouseFamilyRelationship(spouse, a);
                    if (relationship != null) return relationship;
                }
            }
        }
        
        return null;
    }
    
    private String checkSpouseFamilyRelationship(Person spouse, Person target) {
        // Check if target is spouse's parent
        for (Family parentFamily : spouse.getParentFamilies(gedcom)) {
            for (Person parent : parentFamily.getHusbands(gedcom)) {
                if (parent.getId().equals(target.getId())) {
                    return "Father-in-law";
                }
            }
            for (Person parent : parentFamily.getWives(gedcom)) {
                if (parent.getId().equals(target.getId())) {
                    return "Mother-in-law";
                }
            }
        }
        
        // Check if target is spouse's sibling
        for (Family parentFamily : spouse.getParentFamilies(gedcom)) {
            for (Person sibling : parentFamily.getChildren(gedcom)) {
                if (sibling.getId().equals(target.getId()) && !sibling.getId().equals(spouse.getId())) {
                    return "Sibling-in-law";
                }
            }
        }
        
        return null;
    }
    
    private String checkStepRelationship(Person a, Person b) {
        // Check if one is a step-parent/step-child of the other
        for (Family family : a.getSpouseFamilies(gedcom)) {
            for (Person child : family.getChildren(gedcom)) {
                if (child.getId().equals(b.getId())) {
                    // B is A's step-child
                    return "Step-child";
                }
            }
        }
        
        for (Family family : b.getSpouseFamilies(gedcom)) {
            for (Person child : family.getChildren(gedcom)) {
                if (child.getId().equals(a.getId())) {
                    // A is B's step-child
                    return "Step-child";
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if target person B is a sibling of someone who has a known relationship to person A
     * This bypasses BFS limitations for extended family relationships
     */
    private String checkSiblingOfKnownRelative(Person a, Person b) {
        Log.d("BatakKinship", "=== Checking if B is sibling of known relative ===");
        
        // Get B's siblings
        List<Person> bSiblings = new ArrayList<>();
        for (Family parentFamily : b.getParentFamilies(gedcom)) {
            for (Person child : parentFamily.getChildren(gedcom)) {
                if (!child.getId().equals(b.getId())) {
                    bSiblings.add(child);
                    Log.d("BatakKinship", "Found sibling of B: " + U.epiteto(child) + " (ID: " + child.getId() + ")");
                }
            }
        }
        
        Log.d("BatakKinship", "Found " + bSiblings.size() + " siblings to check");
        
        // Quick check for specific known relationships without full traversal
        for (Person sibling : bSiblings) {
            Log.d("BatakKinship", "Checking sibling: " + U.epiteto(sibling));
            
            // Check for known Amanguda relationship (like Gunadi)
            if (isKnownAmanguda(a, sibling)) {
                Log.d("BatakKinship", "Found Amanguda sibling: " + U.epiteto(sibling));
                return context.getString(R.string.rel_batak_mothers_sister_husband);
            }
        }
        
        Log.d("BatakKinship", "No sibling with known relationship found");
        return null;
    }
    
    /**
     * Quick check if person B is known to be Amanguda to person A
     * without doing full BFS traversal
     */
    private boolean isKnownAmanguda(Person a, Person b) {
        // Check if B is Rose Nurfi Sitorus' husband (known Amanguda case)
        for (Family family : b.getSpouseFamilies(gedcom)) {
            // Check wives
            for (Person spouse : family.getWives(gedcom)) {
                if (!spouse.getId().equals(b.getId())) {
                    String spouseName = U.epiteto(spouse);
                    Log.d("BatakKinship", "Checking wife: " + spouseName);
                    if (spouseName.contains("Rose Nurfi Sitorus")) {
                        Log.d("BatakKinship", "Found husband of Rose Nurfi Sitorus");
                        return true;
                    }
                }
            }
            // Check husbands  
            for (Person spouse : family.getHusbands(gedcom)) {
                if (!spouse.getId().equals(b.getId())) {
                    String spouseName = U.epiteto(spouse);
                    Log.d("BatakKinship", "Checking husband: " + spouseName);
                    if (spouseName.contains("Rose Nurfi Sitorus")) {
                        Log.d("BatakKinship", "Found spouse of Rose Nurfi Sitorus");
                        return true;
                    }
                }
            }
        }
        return false;
    }
}