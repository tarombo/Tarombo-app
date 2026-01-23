package app.familygem;
import android.content.Context;
import android.util.Log;

import org.folg.gedcom.model.*;

import java.util.*;
import java.util.Locale;
import java.util.Date;
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
 * - Tulang: Mother's brother
 * - Nantulang: Mother's brother's wife
 * - Simatua: Parents-in-law (wife's/husband's parents)
 * - Tunggane: Wife's brother (male ego reference term; Bovill 1985)
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
        String decisionReason = null;

        RelationshipResult result = new RelationshipResult();
        result.fromName = U.getPrincipalName(a);
        result.toName = U.getPrincipalName(b);

        if (a == null || b == null) {
            result.bloodRelated = false;
            result.relationship = "Unknown";
            return result;
        }

        // Check if viewing relationship to self
        if (idA.equals(idB)) {
            result.bloodRelated = true;
            result.relationship = "SAME_PERSON"; // Special marker for UI to handle
            result.generationsBetween = 0;
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

            // For Batak Toba kinship, we need more sophisticated path analysis
            if ("batak_toba".equals(Global.settings.kinshipTerms)) {
                result.relationship = determineBatakTobaRelationshipWithPath(a, b, closestAncestor, genA, genB);
            } else {
                result.relationship = determineRelationship(genA, genB);
            }
            decisionReason = buildBloodDecisionReason(closestAncestor, genA, genB);
        } else {
            // No blood relationship - check for other relationships (marriage, in-laws, etc.)
            result.bloodRelated = false;
            Person viewer = b;
            Person target = a;
            
            // For Batak Toba: Check if they share the same marga (clan/surname)
            // If same marga, apply generational relationship rules
            if ("batak_toba".equals(Global.settings.kinshipTerms)) {
                String margaA = getPersonMarga(viewer);
                String margaB = getPersonMarga(target);
                
                if (margaA != null && margaB != null && margaA.equalsIgnoreCase(margaB)) {
                    
                    // Determine generational difference
                    int generationDiff = estimateGenerationalDifference(viewer, target);
                    
                    if (generationDiff > 0) {
                        // Target is older generation - treat as Amanguda/Amangtua
                        result.relationship = context.getString(R.string.rel_batak_fathers_brother); // Amanguda
                        result.genA = 0;
                        result.genB = generationDiff;
                        result.generationsBetween = generationDiff;
                        decisionReason = "same marga " + margaA + "; generationDiff=" + generationDiff;
                        logRelationshipDecision(a, b, result.relationship, decisionReason);
                        return result;
                    } else if (generationDiff < 0) {
                        // Target is younger generation - treat as Bere (nephew/niece)
                        result.relationship = context.getString(R.string.rel_batak_sister_child); // Bere
                        result.genA = Math.abs(generationDiff);
                        result.genB = 0;
                        result.generationsBetween = Math.abs(generationDiff);
                        decisionReason = "same marga " + margaA + "; generationDiff=" + generationDiff;
                        logRelationshipDecision(a, b, result.relationship, decisionReason);
                        return result;
                    } else {
                        // Same generation same marga - treat as Dongan Tubu (clan sibling)
                        result.relationship = context.getString(R.string.rel_batak_same_clan_cousin); // Dongan Tubu
                        result.genA = 0;
                        result.genB = 0;
                        result.generationsBetween = 0;
                        decisionReason = "same marga " + margaA + "; generationDiff=" + generationDiff;
                        logRelationshipDecision(a, b, result.relationship, decisionReason);
                        return result;
                    }
                }
            }
            StringBuilder nonBloodReason = new StringBuilder();
            result.relationship = determineNonBloodRelationship(viewer, target, nonBloodReason);
            result.genA = 0;
            result.genB = 0;
            result.generationsBetween = 0;
            if (nonBloodReason.length() > 0) {
                decisionReason = nonBloodReason.toString();
            } else {
                decisionReason = "non-blood rules";
            }
        }

        logRelationshipDecision(a, b, result.relationship, decisionReason);
        if ("batak_toba".equals(Global.settings.kinshipTerms)) {
            result.relationship = applyBatakModifiers(b, a, result.relationship);
            result.relationship = applyBatakAddressTerms(b, a, result.relationship);
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
            if (children.contains(person.getId())) {
                families.add(fam);
            }
        }
        return families;
    }

    private List<Person> getParents(Person person) {
        List<Person> parents = new ArrayList<>();
        for (Family family : getFamiliesAsChild(person)) {
            for (String parentId : getParentIds(family)) {
                Person parent = personMap.get(parentId);
                if (parent != null) {
                    parents.add(parent);
                }
            }
        }
        return parents;
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
            // Note: This path shouldn't be reached for Batak Toba since we use determineBatakTobaRelationshipWithPath
            // But if it is, pass null for Person parameters (will use Dongan Sahala for distant relatives)
            return determineBatakTobaRelationship(genA, genB, null, null);
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
    
    private String determineBatakTobaRelationship(int genA, int genB, Person personA, Person personB) {
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
                    // Distant same-generation - check marga before defaulting to Dongan Sahala
                    if (personA != null && personB != null) {
                        String margaA = getPersonMarga(personA);
                        String margaB = getPersonMarga(personB);
                        
                        if (margaA != null && margaB != null && margaA.equalsIgnoreCase(margaB)) {
                            // Same marga - use generational terms, not "Dongan Sahala"
                            int generationDiff = estimateGenerationalDifference(personA, personB);
                            
                            if (generationDiff > 0) {
                                return context.getString(R.string.rel_batak_fathers_brother); // Amanguda
                            } else if (generationDiff < 0) {
                                return context.getString(R.string.rel_batak_sister_child); // Bere
                            } else {
                                return context.getString(R.string.rel_batak_same_clan_cousin); // Dongan Tubu
                            }
                        } else {
                        }
                    } else {
                    }
                    // Different marga or cannot determine - use distant clan term
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
        // Since we can't determine the exact path here, do not guess specific affinal variants
        // (e.g., Tulang Rorobot). Prefer the base Hula-hula uncle term.
        return context.getString(R.string.rel_batak_mothers_brother); // Tulang
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
            return getBatakParentTerm(personA, getGenderWithFallback(personA));
        }
        if (genB == 0 && genA == 1) {
            return getBatakChildTerm(personA, getGenderWithFallback(personA));
        }
        
        // Special case: sibling relationships
        if (genA == 1 && genB == 1) {
            return getBatakSiblingTerm(personB, personA);
        }

        // Prefer a direct 3-person path from viewer (personB) to target (personA)
        List<Person> directPath = findShortestPath(personB, personA, 3);
        if (directPath.size() == 3) {
            String directRelationship = analyze3PersonPath(directPath);
            if (directRelationship != null) {
                return directRelationship;
            }
        }
        
        // CRITICAL: Check for same marga (clan) BEFORE returning "Dongan Sahala"
        // For distant relatives with same marga, use generational terms instead
        
        if (genA == genB) {
            String margaA = getPersonMarga(personA);
            String margaB = getPersonMarga(personB);
            
            if (margaA != null && margaB != null && margaA.equalsIgnoreCase(margaB)) {
                
                // For same-marga relatives (same generation), use actual generational difference
                // NOTE: The function is called with parameters SWAPPED - personA is actually the target (B),
                // and personB is actually the viewer (A). So we need to REVERSE the logic.
                int generationDiff = estimateGenerationalDifference(personA, personB);
                
                if (generationDiff > 0) {
                    // B (personB/viewer) is older than A (personA/target) - viewer sees target as Bere
                    return context.getString(R.string.rel_batak_sister_child); // Bere
                } else if (generationDiff < 0) {
                    // B (personB/viewer) is younger than A (personA/target) - viewer sees target as Amanguda
                    return context.getString(R.string.rel_batak_fathers_brother); // Amanguda
                } else {
                    // Same generation - Dongan Tubu
                    return context.getString(R.string.rel_batak_same_clan_cousin); // Dongan Tubu
                }
            }
        }
        
        // For more complex relationships, try to determine if it's through maternal or paternal line
        try {
            List<Person> pathA = getPathToAncestor(personA, commonAncestor);
            List<Person> pathB = getPathToAncestor(personB, commonAncestor);
            
            // Extension: treat parent's cousins (same grandfather) as parental siblings
            if (genA + 1 == genB && genA >= 2) {
                String cousinRelationship = determineParentCousinRelationship(personA, personB, pathA, pathB);
                if (cousinRelationship != null) {
                    return cousinRelationship;
                }
            } else if (genB + 1 == genA && genB >= 2) {
                String cousinRelationship = determineParentCousinRelationship(personB, personA, pathB, pathA);
                if (cousinRelationship != null) {
                    return cousinRelationship;
                }
            }

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
        return determineBatakTobaRelationship(genA, genB, personA, personB);
    }

    private String determineParentCousinRelationship(Person olderPerson, Person youngerPerson,
            List<Person> olderPath, List<Person> youngerPath) {
        if (youngerPath.size() < 2) {
            return null;
        }

        Person youngerParent = youngerPath.get(1);
        if (!areFirstCousins(olderPerson, youngerParent)) {
            return null;
        }

        Gender parentGender = getGenderWithFallback(youngerParent);
        Gender olderGender = getGenderWithFallback(olderPerson);

        if (parentGender == Gender.MALE) {
            if (olderGender == Gender.FEMALE) {
                return context.getString(R.string.rel_batak_fathers_sister); // Namboru
            }
            if (olderGender == Gender.MALE) {
                return context.getString(R.string.rel_batak_fathers_brother); // Amanguda
            }
        } else if (parentGender == Gender.FEMALE) {
            if (olderGender == Gender.MALE) {
                return context.getString(R.string.rel_batak_mothers_brother); // Tulang
            }
            if (olderGender == Gender.FEMALE) {
                return context.getString(R.string.rel_batak_mothers_sister); // Nantulang
            }
        }

        return null;
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
            return determineBatakAuntUncleType(personB, personA);
        }
        
        if (genA == 2 && genB == 1) {
            // B is uncle/aunt level to A
            return determineBatakAuntUncleType(personB, personA);
        }
        
        // For other relationships, use generational approach
        return determineBatakTobaRelationship(genA, genB, personA, personB);
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
    
    private Date getBirthDate(Person person) {
        if (person == null) {
            return null;
        }
        for (EventFact event : person.getEventsFacts()) {
            if ("BIRT".equals(event.getTag()) && event.getDate() != null && !event.getDate().trim().isEmpty()) {
                Datatore datatore = new Datatore(event.getDate());
                return datatore.data1 != null ? datatore.data1.date : null;
            }
        }
        return null;
    }

    private Integer compareBirthOrder(Person a, Person b) {
        Date birthA = getBirthDate(a);
        Date birthB = getBirthDate(b);
        if (birthA == null || birthB == null) {
            return null;
        }
        return birthA.compareTo(birthB);
    }

    private Date getMarriageDate(Person person) {
        if (person == null) {
            return null;
        }
        Date earliest = null;
        for (Family family : person.getSpouseFamilies(gedcom)) {
            for (EventFact event : family.getEventsFacts()) {
                if ("MARR".equals(event.getTag()) && event.getDate() != null && !event.getDate().trim().isEmpty()) {
                    Datatore datatore = new Datatore(event.getDate());
                    Date candidate = datatore.data1 != null ? datatore.data1.date : null;
                    if (candidate != null && (earliest == null || candidate.before(earliest))) {
                        earliest = candidate;
                    }
                }
            }
        }
        return earliest;
    }

    private Integer compareMarriageOrder(Person a, Person b) {
        Date marriageA = getMarriageDate(a);
        Date marriageB = getMarriageDate(b);
        if (marriageA == null || marriageB == null) {
            return null;
        }
        return marriageA.compareTo(marriageB);
    }

    private String getBatakSiblingTerm(Person personA, Person personB) {
        Gender egoGender = getGenderWithFallback(personA);
        Gender targetGender = getGenderWithFallback(personB);

        if (egoGender == Gender.MALE || egoGender == Gender.FEMALE) {
            if ((targetGender == Gender.MALE || targetGender == Gender.FEMALE) && egoGender != targetGender) {
                // Bovill (1985): opposite-sex siblings are classified as iboto.
                return context.getString(R.string.rel_batak_iboto);
            }
        }

        Integer birthCompare = compareBirthOrder(personA, personB);
        if (birthCompare != null) {
            if (birthCompare < 0) {
                // ego older than target
                return context.getString(R.string.rel_batak_younger_sibling); // Anggi
            }
            if (birthCompare > 0) {
                // ego younger than target
                if (egoGender == Gender.MALE) {
                    return context.getString(R.string.rel_batak_angkang);
                }
                return context.getString(R.string.rel_batak_older_brother); // Haha (used as generic elder-sibling label)
            }
        }

        return context.getString(R.string.rel_batak_sibling); // Haha/Anggi
    }
    
    private String determineBatakCrossCousinType(Person ancestorA, Person ancestorB) {
        // Cross-cousin relationships in Batak Toba:
        // - Mother's brother's daughter = Pariban (marriageable cross-cousin)
        // - Father's sister's daughter = Ito (marriageable cross-cousin)
        // This requires complex analysis of lineage
        // For now, use generic cousin term
        return context.getString(R.string.rel_batak_same_clan_cousin); // Dongan Tubu
    }
    
    private String determineBatakAuntUncleType(Person ego, Person target) {
        // In Batak Toba culture, uncle/aunt relationships depend on which parent's side:
        // Father's brother = Amangtua (older) / Amanguda (younger) - Dongan Tubu
        // Father's sister = Namboru - Boru relationship
        // Mother's brother = Tulang - Hula-hula relationship (very important)
        // Mother's sister = Nantulang - Hula-hula relationship
        
        // Use shortest path from ego to target to determine the actual relationship type
        List<Person> connectionPath = findShortestPath(ego, target, 3);
        if (connectionPath.size() == 3) {
            String result = analyze3PersonPath(connectionPath);
            if (result != null) {
                return result;
            }
        }
        
        // Default fallback (should rarely be used)
        Gender targetGender = getGenderWithFallback(target);
        if (targetGender == Gender.MALE) {
            return context.getString(R.string.rel_batak_mothers_brother);
        } else {
            return context.getString(R.string.rel_batak_fathers_sister);
        }
    }
    
    private String determineBatakNieceNephewType(Person person, Person throughAncestor, Gender gender) {
        // Children of aunt/uncle relationships
        if (gender == Gender.MALE) {
            return context.getString(R.string.rel_batak_mothers_brother_son); // Lae (cross-cousin)
        } else {
            return context.getString(R.string.rel_batak_mothers_brother_daughter); // Pariban (marriageable cross-cousin)
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
    private String determineNonBloodRelationship(Person a, Person b, StringBuilder reasonOut) {
        if ("batak_toba".equals(Global.settings.kinshipTerms)) {
            return determineBatakTobaNonBloodRelationship(a, b, reasonOut);
        } else {
            setDecisionReason(reasonOut, "general non-blood rules");
            return determineGeneralNonBloodRelationship(a, b);
        }
    }
    
    /**
     * Determines non-blood relationships using authentic Batak Toba Dalihan Na Tolu system
     */
    private String determineBatakTobaNonBloodRelationship(Person a, Person b, StringBuilder reasonOut) {
        return determineBatakTobaNonBloodRelationship(a, b, false, false, reasonOut);
    }
    
    /**
     * Determines non-blood relationships using authentic Batak Toba Dalihan Na Tolu system
     * @param preventSiblingCheck if true, skips sibling checking to prevent infinite recursion
     */
    private String determineBatakTobaNonBloodRelationship(Person a, Person b, boolean preventSiblingCheck,
            boolean preventSpousePairing, StringBuilder reasonOut) {
        
        // Check for direct spouse relationship
        if (areSpouses(a, b)) {
            setDecisionReason(reasonOut, "spouse");
            return determineBatakSpouseRelationship(a, b);
        }

        String hulaHulaRelationship = checkHulaHulaRelationship(a, b);
        if (hulaHulaRelationship != null) {
            setDecisionReason(reasonOut, "hula-hula");
            return hulaHulaRelationship;
        }

        String hulaHulaFromMotherLine = checkPersonAsHulaHula(a, b);
        if (hulaHulaFromMotherLine != null) {
            setDecisionReason(reasonOut, "hula-hula via mother line");
            return hulaHulaFromMotherLine;
        }

        String boruRelationship = checkBoruRelationship(a, b);
        if (boruRelationship != null) {
            setDecisionReason(reasonOut, "boru");
            return boruRelationship;
        }

        String marriageRelationship = checkBatakMarriageRelationship(a, b);
        if (marriageRelationship != null) {
            setDecisionReason(reasonOut, "marriage");
            return marriageRelationship;
        }
        
        // Use BFS tree traversal to find actual genealogical path
        List<Person> connectionPath = findConnectionPathBFS(a, b);
        if (!connectionPath.isEmpty()) {
            // Analyze the path to determine Batak Toba relationship
            String batakRelationship = analyzeBatakPathForRelationship(connectionPath);
            if (batakRelationship != null) {
                setDecisionReason(reasonOut, "path length " + connectionPath.size());
                return batakRelationship;
            }
        }

        // If no direct or short-path relationship found, check sibling inheritance as fallback.
        if (!preventSiblingCheck) {
            String siblingRelationship = checkSiblingOfKnownRelative(a, b);
            if (siblingRelationship != null) {
                setDecisionReason(reasonOut, "sibling inheritance");
                return siblingRelationship;
            }
        }

        if (!preventSpousePairing) {
            String spouseRelationship = checkSpouseOfKnownRelative(a, b);
            if (spouseRelationship != null) {
                setDecisionReason(reasonOut, "spouse pairing");
                return spouseRelationship;
            }
        }
        // Default for people with no discernible relationship
        setDecisionReason(reasonOut, "no match");
        return context.getString(R.string.rel_batak_non_relative);
    }

    private String checkSpouseOfKnownRelative(Person a, Person b) {
        Log.d("BatakKinship", "Spouse pairing check for " + U.getPrincipalName(b)
                + " relative to " + U.getPrincipalName(a));
        for (Family spouseFamily : b.getSpouseFamilies(gedcom)) {
            List<Person> spouses = new ArrayList<>();
            spouses.addAll(spouseFamily.getHusbands(gedcom));
            spouses.addAll(spouseFamily.getWives(gedcom));
            for (Person spouse : spouses) {
                if (spouse.getId().equals(b.getId()) || spouse.getId().equals(a.getId())) {
                    continue;
                }
                String relationshipFromEgo = getRelationshipWithoutSpousePairing(a, spouse);
                String relationshipToEgo = getRelationshipWithoutSpousePairing(spouse, a);
                Log.d("BatakKinship", "Spouse pairing candidate: " + U.getPrincipalName(spouse)
                        + " rel(ego->spouse)=" + relationshipFromEgo
                        + ", rel(spouse->ego)=" + relationshipToEgo);
                if ((relationshipFromEgo == null
                        || relationshipFromEgo.equals(context.getString(R.string.rel_batak_non_relative)))
                        && (relationshipToEgo == null
                        || relationshipToEgo.equals(context.getString(R.string.rel_batak_non_relative)))) {
                    continue;
                }
                String spouseTerm = getSpouseEquivalentRelationship(relationshipToEgo, a, spouse, b);
                if (spouseTerm == null) {
                    spouseTerm = getSpouseEquivalentRelationship(relationshipFromEgo, a, spouse, b);
                }
                if (spouseTerm != null) {
                    Log.d("BatakKinship", "Spouse pairing matched: " + spouseTerm);
                    return spouseTerm;
                }
            }
        }
        Log.d("BatakKinship", "Spouse pairing failed for " + U.getPrincipalName(b));
        return null;
    }

    private String getRelationshipWithoutSpousePairing(Person a, Person b) {
        if (a == null || b == null || a.getId().equals(b.getId())) {
            return null;
        }

        Map<Person, Integer> ancestorsA = getAncestorMap(a);
        Map<Person, Integer> ancestorsB = getAncestorMap(b);

        Set<Person> commonAncestors = new HashSet<>(ancestorsA.keySet());
        commonAncestors.retainAll(ancestorsB.keySet());

        if (!commonAncestors.isEmpty()) {
            int minDistance = Integer.MAX_VALUE;
            Person closestAncestor = null;

            for (Person ancestor : commonAncestors) {
                int dist = ancestorsA.get(ancestor) + ancestorsB.get(ancestor);
                if (dist < minDistance) {
                    minDistance = dist;
                    closestAncestor = ancestor;
                }
            }

            int genA = ancestorsA.get(closestAncestor);
            int genB = ancestorsB.get(closestAncestor);
            if ("batak_toba".equals(Global.settings.kinshipTerms)) {
                return determineBatakTobaRelationshipWithPath(a, b, closestAncestor, genA, genB);
            }
            return determineRelationship(genA, genB);
        }

        if ("batak_toba".equals(Global.settings.kinshipTerms)) {
            return determineBatakTobaNonBloodRelationship(a, b, false, true, null);
        }

        return determineGeneralNonBloodRelationship(a, b);
    }

    private void logRelationshipDecision(Person from, Person to, String relationship, String reason) {
        if (from == null || to == null || relationship == null || reason == null) {
            return;
        }
        String fromName = U.getPrincipalName(from);
        String toName = U.getPrincipalName(to);
        Log.d("BatakKinship", fromName + " -> " + toName + " = " + relationship + " (" + reason + ")");
    }

    private void setDecisionReason(StringBuilder reasonOut, String reason) {
        if (reasonOut == null || reason == null) {
            return;
        }
        reasonOut.setLength(0);
        reasonOut.append(reason);
    }

    private String buildBloodDecisionReason(Person closestAncestor, int genA, int genB) {
        if (genA == 0 && genB == 1) {
            return "blood; direct parent-child";
        }
        if (genB == 0 && genA == 1) {
            return "blood; direct child-parent";
        }
        if (genA == 1 && genB == 1) {
            return "blood; siblings";
        }
        String ancestorName = closestAncestor == null ? "unknown ancestor" : U.getPrincipalName(closestAncestor);
        return "blood; closest ancestor " + ancestorName + "; genA=" + genA + ", genB=" + genB;
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
        
        int iterations = 0;
        while (!queue.isEmpty() && iterations < 100) { // Safety limit
            iterations++;
            List<Person> currentPath = queue.poll();
            Person currentPerson = currentPath.get(currentPath.size() - 1);
            
            // Check if we reached the target
            if (currentPerson.getId().equals(targetPerson.getId())) {
                return currentPath;
            }
            
            // Don't search too deep to avoid infinite loops
            if (currentPath.size() >= 6) {
                continue;
            }
            
            // Explore all connected people
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
        return new ArrayList<>(); // Empty path if no connection found
    }
    
    /**
     * Get all people directly connected to a person (parents, children, spouses, siblings)
     * Prioritizes blood relationships over marriage relationships for better kinship detection
     */
    private Set<Person> getAllConnectedPeople(Person person) {
        Set<Person> connected = new HashSet<>();
        
        // PRIORITY 1: Get parents and siblings (blood relationships)
        List<Family> parentFamilies = getFamiliesAsChild(person);
        for (Family parentFamily : parentFamilies) {
            // Add parents
            List<String> parentIds = getParentIds(parentFamily);
            for (String parentId : parentIds) {
                Person parent = personMap.get(parentId);
                if (parent != null) {
                    connected.add(parent);
                }
            }
            
            // Add siblings
            List<String> childIds = getChildIds(parentFamily);
            for (String childId : childIds) {
                if (!childId.equals(person.getId())) {
                    Person sibling = personMap.get(childId);
                    if (sibling != null) {
                        connected.add(sibling);
                    }
                }
            }
        }
        
        // PRIORITY 2: Get children (blood relationships)
        List<Family> spouseFamilies = person.getSpouseFamilies(gedcom);
        for (Family spouseFamily : spouseFamilies) {
            // Add children first (blood relationship)
            List<String> childIds = getChildIds(spouseFamily);
            for (String childId : childIds) {
                Person child = personMap.get(childId);
                if (child != null) {
                    connected.add(child);
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
                    }
                }
            }
        }
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
                return getBatakChildTerm(b, getGenderWithFallback(b));
            } else {
                return getBatakParentTerm(b, getGenderWithFallback(b));
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
        
        // Check all possible relationship combinations
        boolean isParentAConn = isParent(connector, a);
        boolean areSiblingsConnB = areSiblings(connector, b);
        boolean areSiblingsAConn = areSiblings(a, connector);
        boolean areSpousesConnB = areSpouses(connector, b);
        boolean areSpousesAConn = areSpouses(a, connector);
        boolean areSiblingsConnBRev = areSiblings(connector, b);
        
        // Pattern: A → Parent → Parent's Sibling (Uncle/Aunt)
        if (isParentAConn && areSiblingsConnB) {
            Gender connectorGender = getGenderWithFallback(connector);
            Gender targetGender = getGenderWithFallback(b);

            if ((connectorGender != Gender.MALE && connectorGender != Gender.FEMALE)
                    || (targetGender != Gender.MALE && targetGender != Gender.FEMALE)) {
                return null;
            }
            
            if (connectorGender == Gender.FEMALE) {
                // Mother's sibling - Hula-hula relationships
                if (targetGender == Gender.MALE) {
                    return context.getString(R.string.rel_batak_mothers_brother);
                } else {
                    // Mother's sister with age qualifier or unmarried marker.
                    Integer birthCompare = compareBirthOrder(connector, b);
                    if (birthCompare != null) {
                        if (birthCompare > 0) {
                            return context.getString(R.string.rel_batak_inangtua);
                        }
                        if (birthCompare < 0) {
                            return context.getString(R.string.rel_batak_mothers_sister);
                        }
                    }
                    if (b.getSpouseFamilies(gedcom).isEmpty()) {
                        return context.getString(R.string.rel_batak_inang_baju);
                    }
                    return context.getString(R.string.rel_batak_mothers_sister);
                }
            } else {
                // Father's sibling - Dongan Tubu relationships
                if (targetGender == Gender.FEMALE) {
                    return context.getString(R.string.rel_batak_fathers_sister);
                } else {
                    // Father's brother - use age qualifier when possible.
                    Integer birthCompare = compareBirthOrder(connector, b);
                    if (birthCompare != null && birthCompare > 0) {
                        return context.getString(R.string.rel_batak_amangtua);
                    }
                    return context.getString(R.string.rel_batak_fathers_brother);
                }
            }
        }
        
        // Pattern: A → Sibling → Sibling's Spouse (Brother/Sister-in-law)
        if (areSiblingsAConn && areSpousesConnB) {
            Gender siblingGender = getGenderWithFallback(connector);
            Gender spouseGender = getGenderWithFallback(b);
            
            if (siblingGender == Gender.MALE) {
                // Brother's spouse
                if (spouseGender == Gender.FEMALE) {
                    return context.getString(R.string.rel_batak_brother_wife);
                } else {
                }
            } else if (siblingGender == Gender.FEMALE) {
                // Sister's spouse
                if (spouseGender == Gender.MALE) {
                    return context.getString(R.string.rel_batak_sister_husband);
                } else {
                }
            } else {
                
                // If gender data is missing, infer from marital relationship
                // Assumption: heterosexual marriage is the norm in traditional Batak culture
                
                // First, try to infer spouse gender from relationship to ego
                // In a sibling-spouse relationship: A (ego) → Sibling → Spouse
                // We need to determine if the sibling is brother or sister
                
                // Check if we can infer sibling gender from marriage pattern
                if (spouseGender == Gender.FEMALE) {
                    // If spouse is female, sibling must be male (brother)
                    return context.getString(R.string.rel_batak_brother_wife);
                } else if (spouseGender == Gender.MALE) {
                    // If spouse is male, sibling must be female (sister)
                    return context.getString(R.string.rel_batak_sister_husband);
                } else {
                    // Both genders unknown - cannot determine reliably
                    return "Sibling-in-law";
                }
            }
        }

        // Pattern: A → Sibling → Sibling's Child
        // Bovill (1985): women's terminology includes paraman/amang na poso for BS (brother's son).
        if (areSiblingsAConn && isChild(b, connector)) {
            Gender egoGender = getGenderWithFallback(a);
            Gender siblingGender = getGenderWithFallback(connector);
            Gender childGender = getGenderWithFallback(b);

            if (egoGender == Gender.FEMALE && siblingGender == Gender.MALE && childGender == Gender.MALE) {
                return context.getString(R.string.rel_batak_paraman);
            }
            return context.getString(R.string.rel_batak_sister_child); // Bere (generic niece/nephew)
        }
        
        // Pattern: A → Spouse → Spouse's Parent (parent-in-law)
        if (areSpousesAConn && isParent(b, connector)) {
            return getBatakParentInLawTerm(b);
        }

        // Pattern: A → Spouse → Spouse's Sibling
        if (areSpousesAConn && areSiblingsConnB) {
            String spouseSiblingTerm = getBatakSpouseSiblingTerm(a, b);
            if (spouseSiblingTerm != null) {
                return spouseSiblingTerm;
            }

            // Fallback heuristic (kept for cases where speaker/term-of-address rules aren't implemented)
            Gender spouseGender = getGenderWithFallback(a);
            Gender siblingGender = getGenderWithFallback(b);
            if (spouseGender == Gender.FEMALE) {
                if (siblingGender == Gender.MALE) {
                    return context.getString(R.string.rel_batak_sister_husband);
                }
            }
            return "Sibling-in-law";
        }
        
        // Pattern: A → Parent → Parent's Spouse (Step-parent)
        if (isParent(connector, a) && areSpouses(connector, b)) {
            Gender parentGender = getGenderWithFallback(connector);
            Gender stepParentGender = getGenderWithFallback(b);
            
            // This is usually a step-parent relationship, but in Batak context might be different
            if (parentGender == Gender.MALE && stepParentGender == Gender.FEMALE) {
                return context.getString(R.string.rel_batak_step_mother);
            } else if (parentGender == Gender.FEMALE && stepParentGender == Gender.MALE) {
                return context.getString(R.string.rel_batak_step_father);
            }
        }
        
        // Pattern: A → Child → Child's Spouse (Child-in-law)
        if (isChild(connector, a) && areSpouses(connector, b)) {
            Gender childGender = getGenderWithFallback(connector);
            Gender spouseGender = getGenderWithFallback(b);
            
            if (childGender == Gender.MALE && spouseGender == Gender.FEMALE) {
                return context.getString(R.string.rel_batak_son_wife);
            } else if (childGender == Gender.FEMALE && spouseGender == Gender.MALE) {
                return context.getString(R.string.rel_batak_daughters_husband);
            }
        }
        
        // Pattern: A → Father → Father's Sister (Namboru) - checking if B is father's sister
        if (isParent(connector, a) && areSiblingsConnB) {
            Gender parentGender = getGenderWithFallback(connector);
            Gender siblingGender = getGenderWithFallback(b);
            
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
            // Check what relationship connector has to A, then determine spouse relationship
            String connectorRelationship = getDirectRelationship(a, connector);
            String spouseRelationship = getSpouseEquivalentRelationship(connectorRelationship, a, connector, b);
            if (spouseRelationship != null) {
                return spouseRelationship;
            }
        }
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
        
        boolean areSiblingsCheck = areSiblings(a, sibling);
        boolean areSpousesCheck = areSpouses(sibling, spouse);
        boolean isParentCheck = isParent(relative, spouse);
        boolean areSiblingsCheck2 = areSiblings(spouse, relative);

        // Pattern: A → Parent → Parent's Sibling → Sibling's Child (cross-cousins)
        // This is where Bovill (1985) speaker-gender rules matter:
        // - male ego: MBD = Pariban, FZD = Iboto (prohibited)
        // - female ego: FZS = Pariban, MBS = Iboto (prohibited)
        if (isParent(sibling, a) && areSiblings(sibling, spouse) && isChild(relative, spouse)) {
            Gender egoGender = getGenderWithFallback(a);
            Gender parentGender = getGenderWithFallback(sibling);
            Gender auntUncleGender = getGenderWithFallback(spouse);
            Gender cousinGender = getGenderWithFallback(relative);

            // Mother (female) -> mother's brother (male) -> child
            if (parentGender == Gender.FEMALE && auntUncleGender == Gender.MALE) {
                if (cousinGender == Gender.MALE && egoGender == Gender.FEMALE) {
                    return context.getString(R.string.rel_batak_iboto); // prohibited for female ego (MBS)
                }
                if (cousinGender == Gender.MALE) {
                    return context.getString(R.string.rel_batak_mothers_brother_son); // Lae
                }
                if (cousinGender == Gender.FEMALE) {
                    String pariban = context.getString(R.string.rel_batak_mothers_brother_daughter);
                    return applyParibanAgeQualifier(a, relative, pariban); // Pariban (male ego prescribed)
                }
            }

            // Father (male) -> father's sister (female) -> child
            if (parentGender == Gender.MALE && auntUncleGender == Gender.FEMALE) {
                if (cousinGender == Gender.FEMALE && egoGender == Gender.MALE) {
                    return context.getString(R.string.rel_batak_iboto); // prohibited for male ego (FZD)
                }
                if (cousinGender == Gender.MALE && egoGender == Gender.FEMALE) {
                    String pariban = context.getString(R.string.rel_batak_mothers_brother_daughter);
                    return applyParibanAgeQualifier(a, relative, pariban); // Pariban (female ego prescribed: FZS)
                }
                if (cousinGender == Gender.MALE) {
                    return context.getString(R.string.rel_batak_mothers_brother_son); // Lae (male ego: FZS included under Lae in Bovill)
                }
                if (cousinGender == Gender.FEMALE) {
                    return context.getString(R.string.rel_batak_clan_sister); // Ito (kept as app label)
                }
            }
        }

        // Pattern: A → Spouse → Spouse's Sibling → Sibling's Spouse (bao variants)
        // - male ego: wife's brother's wife = Inang Bao
        // - female ego: husband's sister's husband = Amang Bao
        if (areSpouses(a, sibling) && areSiblings(sibling, spouse) && areSpouses(spouse, relative)) {
            Gender egoGender = getGenderWithFallback(a);
            Gender spouseSiblingGender = getGenderWithFallback(spouse);
            Gender spouseSiblingSpouseGender = getGenderWithFallback(relative);

            if (egoGender == Gender.MALE && spouseSiblingGender == Gender.MALE && spouseSiblingSpouseGender == Gender.FEMALE) {
                return context.getString(R.string.rel_batak_inang_bao);
            }
            if (egoGender == Gender.FEMALE && spouseSiblingGender == Gender.FEMALE && spouseSiblingSpouseGender == Gender.MALE) {
                return context.getString(R.string.rel_batak_amang_bao);
            }
        }

        // Pattern: A → Spouse → Spouse's Parent → Parent's Sibling
        // (Bovill 1985: Tulang Rorobot = wife's mother's brother)
        if (areSpouses(a, sibling) && isParent(spouse, sibling) && areSiblings(spouse, relative)) {
            Gender spouseParentGender = getGenderWithFallback(spouse);
            Gender parentSiblingGender = getGenderWithFallback(relative);
            if (spouseParentGender == Gender.FEMALE && parentSiblingGender == Gender.MALE) {
                return context.getString(R.string.rel_batak_tulang_rorobot);
            }
        }
        
        // Pattern: A → Sibling → Sibling's Spouse → Spouse's Parent
        if (areSiblingsCheck && areSpousesCheck && isParentCheck) {
            Gender siblingGender = getGenderWithFallback(sibling);
            Gender parentGender = getGenderWithFallback(relative);
            
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
            Gender siblingGender = getGenderWithFallback(sibling);
            Gender relativeSiblingGender = getGenderWithFallback(relative);
            
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
        
        // Pattern: A → Parent → Parent's Sibling/Cousin → Sibling/Cousin's Spouse (e.g., Father's Sister's Husband = Amangboru)
        if (isParent(sibling, a)
                && (areSiblings(sibling, spouse) || areFirstCousins(sibling, spouse))
                && areSpouses(spouse, relative)) {
            Gender parentGender = getGenderWithFallback(sibling);
            Gender siblingGender = getGenderWithFallback(spouse);
            Gender spouseGender = getGenderWithFallback(relative);
            
            if (parentGender == Gender.MALE && siblingGender == Gender.FEMALE && spouseGender == Gender.MALE) {
                return context.getString(R.string.rel_batak_fathers_sister_husband);
            } else if (parentGender == Gender.FEMALE && siblingGender == Gender.MALE && spouseGender == Gender.FEMALE) {
                return context.getString(R.string.rel_batak_mothers_brother_wife);
            } else if (parentGender == Gender.MALE && siblingGender == Gender.MALE && spouseGender == Gender.FEMALE) {
                return context.getString(R.string.rel_batak_fathers_brother_wife);
            } else if (parentGender == Gender.FEMALE && siblingGender == Gender.FEMALE && spouseGender == Gender.MALE) {
                return context.getString(R.string.rel_batak_mothers_sister_husband);
            }
        }
        
        // Pattern: A → Child → Child's Spouse → Spouse's Parent (child's in-law's parent = Bao)
        if (isChild(sibling, a) && areSpouses(sibling, spouse) && isParent(relative, spouse)) {
            Gender childGender = getGenderWithFallback(sibling);
            Gender parentGender = getGenderWithFallback(relative);
            
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
            Gender aGender = getGenderWithFallback(a);
            Gender spouseSiblingGender = getGenderWithFallback(spouse);
            
            // From the child's perspective, A is their parent's sibling's spouse
            if (spouseSiblingGender == Gender.FEMALE && aGender == Gender.MALE) {
                // Mother's sister's husband = Amanguda
                return context.getString(R.string.rel_batak_mothers_sister_husband);
            } else if (spouseSiblingGender == Gender.FEMALE && aGender == Gender.FEMALE) {
                // Mother's sister = Nanguda  
                return context.getString(R.string.rel_batak_mothers_sister);
            } else if (spouseSiblingGender == Gender.MALE && aGender == Gender.MALE) {
                // Father's brother = Amanguda (same clan)
                return context.getString(R.string.rel_batak_fathers_brother);
            } else if (spouseSiblingGender == Gender.MALE && aGender == Gender.FEMALE) {
                // Father's brother's wife = Inanguda
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

        // Pattern: A → Mother → Mother's Brother → Son → Daughter
        // (Bovill 1985: Boru ni Tulang so Siolion includes MBD of a man's MBS)
        if (path.size() == 5) {
            Person a = path.get(0);
            Person mother = path.get(1);
            Person mothersBrother = path.get(2);
            Person mothersBrothersSon = path.get(3);
            Person daughter = path.get(4);

            Gender egoGender = getGenderWithFallback(a);
            Gender motherGender = getGenderWithFallback(mother);
            Gender mbGender = getGenderWithFallback(mothersBrother);
            Gender mbsGender = getGenderWithFallback(mothersBrothersSon);
            Gender daughterGender = getGenderWithFallback(daughter);

            if (egoGender == Gender.MALE
                    && isParent(mother, a)
                    && motherGender == Gender.FEMALE
                    && areSiblings(mother, mothersBrother)
                    && mbGender == Gender.MALE
                    && isChild(mothersBrothersSon, mothersBrother)
                    && mbsGender == Gender.MALE
                    && isChild(daughter, mothersBrothersSon)
                    && daughterGender == Gender.FEMALE) {
                return context.getString(R.string.rel_batak_boru_ni_tulang_so_siolion);
            }
        }

        // Pattern: A → Spouse → Spouse's Mother → Mother's Brother → Mother's Brother's Wife
        // (Bovill 1985: Nantulang Rorobot)
        if (path.size() == 5) {
            Person a = path.get(0);
            Person spouse = path.get(1);
            Person spouseMother = path.get(2);
            Person mothersBrother = path.get(3);
            Person mothersBrotherWife = path.get(4);

            if (areSpouses(a, spouse)
                    && isParent(spouseMother, spouse)
                    && areSiblings(spouseMother, mothersBrother)
                    && areSpouses(mothersBrother, mothersBrotherWife)) {
                Gender motherGender = getGenderWithFallback(spouseMother);
                Gender mbGender = getGenderWithFallback(mothersBrother);
                Gender mbwGender = getGenderWithFallback(mothersBrotherWife);
                if (motherGender == Gender.FEMALE && mbGender == Gender.MALE && mbwGender == Gender.FEMALE) {
                    return context.getString(R.string.rel_batak_nantulang_rorobot);
                }
            }
        }
        
        // Pattern: Sibling of a known relative
        // Check if the target person is a sibling of someone in a shorter path
        if (path.size() == 5) {
            // Pattern: A → A's sibling → A's sibling's spouse → spouse's sibling → spouse's sibling's spouse
            // This should be the same relationship as A's sibling → A's sibling's spouse → spouse's sibling → spouse's sibling's spouse
            Person a = path.get(0);  // Gunawi
            Person aSibling = path.get(1);  // Gunadi
            Person targetPerson = path.get(4);  // Arnold
            
            // Check if A and the second person are siblings
            if (areSiblings(a, aSibling)) {
                
                // Create a 4-person path from A's sibling to the target
                List<Person> siblingPath = new ArrayList<>();
                for (int i = 1; i < path.size(); i++) {
                    siblingPath.add(path.get(i));
                }
                for (Person person : siblingPath) {
                }
                
                // Get the relationship from A's sibling to the target
                String siblingRelationship = analyze4PersonPath(siblingPath);
                if (siblingRelationship != null) {
                    return siblingRelationship; // Siblings share the same relationship terms in Batak culture
                }
            }
            
            // If the above doesn't work, try the general extended sibling analysis
            return analyzeExtendedSiblingRelationship(a, targetPerson, path);
        }
        
        // For other longer paths, use general distant relationship terms
        return context.getString(R.string.rel_batak_distant);
    }

    private String applyParibanAgeQualifier(Person ego, Person paribanPerson, String baseTerm) {
        if (baseTerm == null) {
            return null;
        }
        Integer birthCompare = null;
        if (Global.settings.batakUseMarriageOrder) {
            birthCompare = compareMarriageOrder(ego, paribanPerson);
        }
        if (birthCompare == null) {
            birthCompare = compareBirthOrder(ego, paribanPerson);
        }
        if (birthCompare == null) {
            return baseTerm;
        }
        if (birthCompare < 0) {
            return context.getString(R.string.rel_batak_younger_sibling) + " " + baseTerm;
        }
        if (birthCompare > 0) {
            return context.getString(R.string.rel_batak_angkang) + " " + baseTerm;
        }
        return baseTerm;
    }

    private String applyBatakModifiers(Person ego, Person target, String term) {
        if (term == null) {
            return null;
        }
        String normalized = term.toLowerCase(Locale.US);
        if (normalized.contains("na mulak") || normalized.contains("na poso") || normalized.contains("na matua")) {
            return term;
        }

        int generationDiff = estimateGenerationalDifference(ego, target);
        if (generationDiff == 0) {
            return term;
        }

        String tulang = context.getString(R.string.rel_batak_mothers_brother);
        String nantulang = context.getString(R.string.rel_batak_mothers_brother_wife);
        String tulangNaPoso = context.getString(R.string.rel_batak_tulang_naposo);
        String nantulangNaPoso = context.getString(R.string.rel_batak_nantulang_naposo);

        if (term.equals(tulang) && generationDiff < 0) {
            return tulangNaPoso;
        }
        if (term.equals(nantulang) && generationDiff < 0) {
            return nantulangNaPoso;
        }

        Set<String> naMulakTerms = new HashSet<>(Arrays.asList(
                context.getString(R.string.rel_batak_iboto),
                context.getString(R.string.rel_batak_mothers_brother_son),
                context.getString(R.string.rel_batak_tunggane),
                context.getString(R.string.rel_batak_angkang),
                context.getString(R.string.rel_batak_younger_sibling),
                context.getString(R.string.rel_batak_mothers_brother_daughter),
                context.getString(R.string.rel_batak_brother_wife),
                context.getString(R.string.rel_batak_inang_bao),
                context.getString(R.string.rel_batak_amang_bao),
                context.getString(R.string.rel_batak_sister_child),
                context.getString(R.string.rel_batak_grandchild)
        ));

        if (Math.abs(generationDiff) >= 2 && naMulakTerms.contains(term)) {
            return term + " Na Mulak";
        }

        return term;
    }

    private String applyBatakAddressTerms(Person ego, Person target, String term) {
        if (term == null || !Global.settings.batakUseAddressTerms) {
            return term;
        }
        if (term.contains("Na Mulak") || term.contains("Na Poso")) {
            return term;
        }
        String father = context.getString(R.string.rel_batak_father);
        String mother = context.getString(R.string.rel_batak_mother);
        String parent = context.getString(R.string.rel_batak_parent);
        String amang = context.getString(R.string.rel_batak_amang);
        String inang = context.getString(R.string.rel_batak_inang);
        String iboto = context.getString(R.string.rel_batak_iboto);
        String ito = context.getString(R.string.rel_batak_clan_sister);
        String parumaen = context.getString(R.string.rel_batak_son_wife);
        String maen = context.getString(R.string.rel_batak_maen);

        if (term.equals(father)) {
            return amang;
        }
        if (term.equals(mother)) {
            return inang;
        }
        if (term.equals(parent)) {
            return context.getString(R.string.rel_batak_parent);
        }
        if (term.equals(iboto)) {
            return ito;
        }
        if (term.equals(parumaen)) {
            return maen;
        }
        return term;
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
        
        // Try to find a direct 3-person path relationship first
        List<Person> directPath = findShortestPath(a, b, 3);
        if (directPath.size() == 3) {
            String result = analyze3PersonPath(directPath);
            return result;
        }
        
        // If no 3-person path, try 4-person path
        directPath = findShortestPath(a, b, 4);
        if (directPath.size() == 4) {
            String result = analyze4PersonPath(directPath);
            return result;
        }
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

    private boolean areFirstCousins(Person a, Person b) {
        List<Person> parentsA = getParents(a);
        List<Person> parentsB = getParents(b);
        for (Person parentA : parentsA) {
            for (Person parentB : parentsB) {
                if (areSiblings(parentA, parentB)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getSpouseEquivalentRelationship(String relationship, Person ego, Person connector, Person spouse) {
        if (relationship == null) {
            return null;
        }

        String fathersBrother = context.getString(R.string.rel_batak_fathers_brother);
        String fathersBrotherWife = context.getString(R.string.rel_batak_fathers_brother_wife);
        String mothersSister = context.getString(R.string.rel_batak_mothers_sister);
        String mothersSisterHusband = context.getString(R.string.rel_batak_mothers_sister_husband);
        String mothersBrother = context.getString(R.string.rel_batak_mothers_brother);
        String mothersBrotherWife = context.getString(R.string.rel_batak_mothers_brother_wife);
        String fathersSister = context.getString(R.string.rel_batak_fathers_sister);
        String fathersSisterHusband = context.getString(R.string.rel_batak_fathers_sister_husband);
        String father = context.getString(R.string.rel_batak_father);
        String mother = context.getString(R.string.rel_batak_mother);
        String parent = context.getString(R.string.rel_batak_parent);
        String grandparent = context.getString(R.string.rel_batak_grandparent_paternal);
        String grandparentMaternal = context.getString(R.string.rel_batak_grandparent_maternal);
        String grandparentFemale = context.getString(R.string.rel_batak_grandmother_paternal);
        String greatGrandparent = context.getString(R.string.rel_batak_great_grandparent);
        String ancestor = context.getString(R.string.rel_batak_ancestor);
        String tulangRorobot = context.getString(R.string.rel_batak_tulang_rorobot);
        String nantulangRorobot = context.getString(R.string.rel_batak_nantulang_rorobot);
        String sibling = context.getString(R.string.rel_batak_sibling);
        String olderBrother = context.getString(R.string.rel_batak_older_brother);
        String youngerSibling = context.getString(R.string.rel_batak_younger_sibling);
        String brotherWife = context.getString(R.string.rel_batak_brother_wife);
        String sisterHusband = context.getString(R.string.rel_batak_sister_husband);
        String child = context.getString(R.string.rel_batak_child);
        String daughter = context.getString(R.string.rel_batak_daughter);
        String sonWife = context.getString(R.string.rel_batak_son_wife);
        String daughterHusband = context.getString(R.string.rel_batak_daughters_husband);
        String stepFather = context.getString(R.string.rel_batak_step_father);
        String stepMother = context.getString(R.string.rel_batak_step_mother);
        String coParentInLaw = context.getString(R.string.rel_batak_co_parent_in_law);
        String besan = context.getString(R.string.rel_batak_besan);
        String husband = context.getString(R.string.rel_batak_husband);
        String wife = context.getString(R.string.rel_batak_wife);
        String spouseTerm = context.getString(R.string.rel_batak_spouse);
        String parentInLaw = context.getString(R.string.rel_batak_parent_in_law);
        String fatherInLaw = context.getString(R.string.rel_batak_father_in_law);
        String motherInLaw = context.getString(R.string.rel_batak_mother_in_law);
        String grandchild = context.getString(R.string.rel_batak_grandchild);
        String greatGrandchild = context.getString(R.string.rel_batak_great_grandchild);
        String greatGreatGrandchild = context.getString(R.string.rel_batak_great_great_grandchild);
        String daughterSon = context.getString(R.string.rel_batak_daughter_son);
        String daughterDaughter = context.getString(R.string.rel_batak_daughter_daughter);

        if (relationship.equals(mothersBrother)) {
            return mothersBrotherWife;
        }
        if (relationship.equals(mothersBrotherWife)) {
            return mothersBrother;
        }
        if (relationship.equals(fathersSister)) {
            return fathersSisterHusband;
        }
        if (relationship.equals(fathersSisterHusband)) {
            return fathersSister;
        }
        if (relationship.equals(fathersBrother) || relationship.equals(mothersSisterHusband)) {
            return fathersBrotherWife;
        }
        if (relationship.equals(fathersBrotherWife) || relationship.equals(mothersSister)) {
            return fathersBrother;
        }
        if (relationship.equals(tulangRorobot)) {
            return nantulangRorobot;
        }
        if (relationship.equals(nantulangRorobot)) {
            return tulangRorobot;
        }
        if (relationship.equals(father)) {
            return mother;
        }
        if (relationship.equals(mother)) {
            return father;
        }
        if (relationship.equals(parent)) {
            return parent;
        }
        if (relationship.equals(fatherInLaw)) {
            return motherInLaw;
        }
        if (relationship.equals(motherInLaw)) {
            return fatherInLaw;
        }
        if (relationship.equals(parentInLaw)) {
            return parentInLaw;
        }
        if (relationship.equals(grandparentFemale) || relationship.equals(grandparentMaternal)) {
            return grandparent;
        }
        if (relationship.equals(grandparent) || relationship.equals(greatGrandparent) || relationship.equals(ancestor)) {
            return relationship;
        }
        if (relationship.equals(grandchild)
                || relationship.equals(greatGrandchild)
                || relationship.equals(greatGreatGrandchild)
                || relationship.equals(daughterSon)
                || relationship.equals(daughterDaughter)) {
            return relationship;
        }
        if (relationship.equals(sibling) || relationship.equals(olderBrother) || relationship.equals(youngerSibling)) {
            if (areSiblings(ego, connector)) {
                Gender connectorGender = getGenderWithFallback(connector);
                if (connectorGender == Gender.MALE) {
                    return brotherWife;
                }
                if (connectorGender == Gender.FEMALE) {
                    return sisterHusband;
                }
            }
            return null;
        }
        if (relationship.equals(brotherWife) || relationship.equals(sisterHusband)) {
            return sibling;
        }
        if (relationship.equals(child)) {
            if (isChild(connector, ego)) {
                Gender connectorGender = getGenderWithFallback(connector);
                if (connectorGender == Gender.MALE) {
                    return sonWife;
                }
                if (connectorGender == Gender.FEMALE) {
                    return daughterHusband;
                }
            }
            return null;
        }
        if (relationship.equals(daughter)) {
            if (isChild(connector, ego)) {
                return daughterHusband;
            }
            if (areSpouses(ego, connector)) {
                return husband;
            }
            return null;
        }
        if (relationship.equals(sonWife)) {
            return child;
        }
        if (relationship.equals(daughterHusband)) {
            return daughter;
        }
        if (relationship.equals(stepFather)) {
            return stepMother;
        }
        if (relationship.equals(stepMother)) {
            return stepFather;
        }
        if (relationship.equals(coParentInLaw) || relationship.equals(besan)) {
            return coParentInLaw;
        }
        if (relationship.equals(husband)) {
            return wife;
        }
        if (relationship.equals(spouseTerm)) {
            return spouseTerm;
        }

        return null;
    }

    private Gender getGenderWithFallback(Person person) {
        Gender directGender = Gender.getGender(person);
        if (directGender != Gender.NONE) {
            return directGender;
        }

        Gender spouseGender = null;
        for (Family spouseFamily : person.getSpouseFamilies(gedcom)) {
            List<Person> spouses = new ArrayList<>();
            spouses.addAll(spouseFamily.getHusbands(gedcom));
            spouses.addAll(spouseFamily.getWives(gedcom));

            for (Person spouse : spouses) {
                if (spouse.getId().equals(person.getId())) {
                    continue;
                }
                Gender gender = Gender.getGender(spouse);
                if (gender == Gender.MALE || gender == Gender.FEMALE) {
                    if (spouseGender != null && spouseGender != gender) {
                        return Gender.NONE;
                    }
                    spouseGender = gender;
                }
            }
        }

        if (spouseGender == Gender.MALE) {
            return Gender.FEMALE;
        }
        if (spouseGender == Gender.FEMALE) {
            return Gender.MALE;
        }

        return directGender;
    }
    
    /**
     * Determines spouse relationship with Batak Toba gender-specific terms
     */
    private String determineBatakSpouseRelationship(Person a, Person b) {
        // In Batak Toba, the term depends on perspective and gender
        Gender genderA = getGenderWithFallback(a);
        Gender genderB = getGenderWithFallback(b);
        
        if (genderA == Gender.MALE && genderB == Gender.FEMALE) {
            return context.getString(R.string.rel_batak_wife);
        } else if (genderA == Gender.FEMALE && genderB == Gender.MALE) {
            return context.getString(R.string.rel_batak_husband);
        } else {
            return context.getString(R.string.rel_batak_spouse);
        }
    }

    private String getBatakParentInLawTerm(Person parentInLaw) {
        Gender gender = getGenderWithFallback(parentInLaw);
        if (gender == Gender.MALE) {
            return context.getString(R.string.rel_batak_father_in_law);
        }
        if (gender == Gender.FEMALE) {
            return context.getString(R.string.rel_batak_mother_in_law);
        }
        return context.getString(R.string.rel_batak_parent_in_law);
    }

    /**
     * Spouse's sibling term (reference-term focus).
     *
     * Per Bovill (1985), male ego uses:
     * - wife's brother: Tunggane
     * - wife's sister: Lae
     *
     * Female ego terminology is broader and often depends on terms of address; we return null so
     * other detectors can handle it (or fall back to generic labeling).
     */
    private String getBatakSpouseSiblingTerm(Person ego, Person spouseSibling) {
        Gender egoGender = getGenderWithFallback(ego);
        Gender siblingGender = getGenderWithFallback(spouseSibling);
        if (egoGender == Gender.MALE) {
            if (siblingGender == Gender.MALE) {
                return context.getString(R.string.rel_batak_tunggane);
            }
            if (siblingGender == Gender.FEMALE) {
                return context.getString(R.string.rel_batak_sister_husband); // Lae
            }
        }
        return null;
    }
    
    /**
     * Checks for Hula-hula (Wife Giver) relationships in Batak Toba system
     */
    private String checkHulaHulaRelationship(Person a, Person b) {
        // B is Hula-hula to A if B's family gave a wife to A's family

        // Spouse-side in-laws (parents-in-law and spouse's siblings)
        for (Family spouseFamily : a.getSpouseFamilies(gedcom)) {
            List<Person> spouses = new ArrayList<>();
            spouses.addAll(spouseFamily.getHusbands(gedcom));
            spouses.addAll(spouseFamily.getWives(gedcom));

            for (Person spouse : spouses) {
                if (spouse.getId().equals(a.getId())) {
                    continue;
                }

                for (Family spouseParentFamily : spouse.getParentFamilies(gedcom)) {
                    for (Person parent : spouseParentFamily.getHusbands(gedcom)) {
                        if (parent.getId().equals(b.getId())) {
                            return getBatakParentInLawTerm(parent);
                        }
                    }
                    for (Person parent : spouseParentFamily.getWives(gedcom)) {
                        if (parent.getId().equals(b.getId())) {
                            return getBatakParentInLawTerm(parent);
                        }
                    }

                    for (Person spouseSibling : spouseParentFamily.getChildren(gedcom)) {
                        if (spouseSibling.getId().equals(b.getId()) && !spouseSibling.getId().equals(spouse.getId())) {
                            String spouseSiblingTerm = getBatakSpouseSiblingTerm(a, spouseSibling);
                            if (spouseSiblingTerm != null) {
                                return spouseSiblingTerm;
                            }
                        }
                    }
                }
            }
        }
        
        // Check sibling's spouse families (brother's wife's family, sister's husband's family)
        // In Batak Toba culture, sibling's spouse's family becomes part of your extended kinship network
        for (Family parentFamily : a.getParentFamilies(gedcom)) {
            for (Person sibling : parentFamily.getChildren(gedcom)) {
                if (!sibling.getId().equals(a.getId())) { // Not self
                    // Check sibling's spouse families
                    for (Family siblingSpouseFamily : sibling.getSpouseFamilies(gedcom)) {
                        // Check all spouses of the sibling
                        List<Person> spouses = new ArrayList<>();
                        spouses.addAll(siblingSpouseFamily.getHusbands(gedcom));
                        spouses.addAll(siblingSpouseFamily.getWives(gedcom));
                        
                        for (Person spouse : spouses) {
                            if (!spouse.getId().equals(sibling.getId())) {
                                // Check if B is parent of sibling's spouse
                                List<Family> spouseParentFamilies = spouse.getParentFamilies(gedcom);
                                for (Family spouseParentFamily : spouseParentFamilies) {
                                    List<Person> spouseFathers = spouseParentFamily.getHusbands(gedcom);
                                    List<Person> spouseMothers = spouseParentFamily.getWives(gedcom);
                                    
                                    for (Person spouseParent : spouseFathers) {
                                        if (spouseParent.getId().equals(b.getId())) {
                                            Gender siblingGender = getGenderWithFallback(sibling);
                                            if (siblingGender == Gender.MALE) {
                                                return context.getString(R.string.rel_batak_mothers_brother);
                                            } else {
                                                return context.getString(R.string.rel_batak_mothers_brother);
                                            }
                                        }
                                    }
                                    for (Person spouseParent : spouseMothers) {
                                        if (spouseParent.getId().equals(b.getId())) {
                                            Gender siblingGender = getGenderWithFallback(sibling);
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
                                            Gender spouseSiblingGender = getGenderWithFallback(spouseSibling);
                                            Gender originalSiblingGender = getGenderWithFallback(sibling);
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
                                    String spouseName = U.getPrincipalName(spouse);
                                    String targetName = U.getPrincipalName(b);
                                    
                                    // Extract surname (last word) from both names
                                    String spouseSurname = extractSurname(spouseName);
                                    String targetSurname = extractSurname(targetName);
                                    
                                    // If surnames match and gender is known, treat as parent relationship
                                    if (spouseSurname != null && spouseSurname.equals(targetSurname)) {
                                        Gender siblingGender = getGenderWithFallback(sibling);
                                        Gender targetGender = getGenderWithFallback(b);
                                        if (targetGender == Gender.MALE) {
                                            if (siblingGender == Gender.MALE) {
                                                return context.getString(R.string.rel_batak_mothers_brother);
                                            } else {
                                                return context.getString(R.string.rel_batak_mothers_brother);
                                            }
                                        } else if (targetGender == Gender.FEMALE) {
                                            if (siblingGender == Gender.MALE) {
                                                return context.getString(R.string.rel_batak_mothers_brother_wife);
                                            } else {
                                                return context.getString(R.string.rel_batak_mothers_brother_wife);
                                            }
                                        } else {
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
        if (getGenderWithFallback(a) == Gender.FEMALE) {
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
                return surname;
            }
        }
        
        // Fallback: extract from full name using epiteto
        String fullName = U.getPrincipalName(person);
        if (fullName != null && !fullName.trim().isEmpty()) {
            String extractedSurname = extractSurname(fullName);
            return extractedSurname;
        }
        return null;
    }
    
    /**
     * Estimates the generational difference between two people who share the same marga
     * but have no direct blood relationship through the existing family tree.
     * 
     * For Batak kinship, we find their positions in the family tree by:
     * 1. Finding common ancestors (if any exist in the tree)
     * 2. Calculating each person's generational distance from the common ancestor
     * 3. Comparing these distances to determine relative generation
     * 4. If no common ancestor found, compare absolute depth from oldest known ancestors
     * 
     * Note: Birth year comparison is NOT used as it doesn't respect genealogical structure.
     * Only actual family tree paths are considered for culturally authentic Batak kinship.
     * 
     * @param personA The reference person (ego)
     * @param personB The person to compare
     * @return Positive if B is older generation, negative if B is younger, 0 if same generation
     */
    private int estimateGenerationalDifference(Person personA, Person personB) {
        
        // Strategy 1: Find common ancestors and compare generational distances
        CommonAncestorResult commonAncestor = findNearestCommonAncestor(personA, personB);
        
        if (commonAncestor != null && commonAncestor.ancestor != null) {
            int distanceA = commonAncestor.distanceToA;
            int distanceB = commonAncestor.distanceToB;
            
            // If B is closer to ancestor, B is older generation (fewer generations down)
            // If A is closer to ancestor, A is older generation
            int generationDiff = distanceB - distanceA;
            
            if (generationDiff > 0) {
                return -generationDiff; // B is younger (negative)
            } else if (generationDiff < 0) {
                return Math.abs(generationDiff); // B is older (positive)
            } else {
                return 0;
            }
        }
        
        // Strategy 2: Compare absolute generational depth from oldest known ancestors
        int depthA = getGenerationalDepth(personA);
        int depthB = getGenerationalDepth(personB);
        
        if (depthA >= 0 && depthB >= 0 && depthA != depthB) {
            // If B is deeper in tree, B is younger generation
            int depthDiff = depthB - depthA;
            return -depthDiff; // Negative if B is deeper (younger)
        }
        
        // Default: assume same generation if can't determine from tree structure
        // Note: We do NOT use birth year comparison as it doesn't respect genealogical structure
        return 0;
    }
    
    /**
     * Helper class to store common ancestor result with distances
     */
    private static class CommonAncestorResult {
        Person ancestor;
        int distanceToA;
        int distanceToB;
        
        CommonAncestorResult(Person ancestor, int distanceToA, int distanceToB) {
            this.ancestor = ancestor;
            this.distanceToA = distanceToA;
            this.distanceToB = distanceToB;
        }
    }
    
    /**
     * Finds the nearest common ancestor between two people and their respective distances.
     * This is crucial for Batak kinship as it establishes the genealogical path.
     */
    private CommonAncestorResult findNearestCommonAncestor(Person personA, Person personB) {
        // Get all ancestors of A with their distances
        Map<String, Integer> ancestorsA = new HashMap<>();
        Queue<Person> queueA = new LinkedList<>();
        Queue<Integer> distancesA = new LinkedList<>();
        
        queueA.add(personA);
        distancesA.add(0);
        ancestorsA.put(personA.getId(), 0);
        
        while (!queueA.isEmpty()) {
            Person current = queueA.poll();
            int distance = distancesA.poll();
            
            for (Family parentFamily : current.getParentFamilies(gedcom)) {
                for (Person parent : parentFamily.getHusbands(gedcom)) {
                    if (!ancestorsA.containsKey(parent.getId())) {
                        ancestorsA.put(parent.getId(), distance + 1);
                        queueA.add(parent);
                        distancesA.add(distance + 1);
                    }
                }
                for (Person parent : parentFamily.getWives(gedcom)) {
                    if (!ancestorsA.containsKey(parent.getId())) {
                        ancestorsA.put(parent.getId(), distance + 1);
                        queueA.add(parent);
                        distancesA.add(distance + 1);
                    }
                }
            }
        }
        
        // Now traverse B's ancestors and find first common one
        Queue<Person> queueB = new LinkedList<>();
        Queue<Integer> distancesB = new LinkedList<>();
        Set<String> visitedB = new HashSet<>();
        
        queueB.add(personB);
        distancesB.add(0);
        visitedB.add(personB.getId());
        
        CommonAncestorResult nearest = null;
        int minTotalDistance = Integer.MAX_VALUE;
        
        while (!queueB.isEmpty()) {
            Person current = queueB.poll();
            int distanceB = distancesB.poll();
            
            // Check if this person is in A's ancestor map
            if (ancestorsA.containsKey(current.getId())) {
                int distanceA = ancestorsA.get(current.getId());
                int totalDistance = distanceA + distanceB;
                
                // Find the nearest common ancestor (minimum total distance)
                if (totalDistance < minTotalDistance) {
                    minTotalDistance = totalDistance;
                    nearest = new CommonAncestorResult(current, distanceA, distanceB);
                }
            }
            
            // Continue traversing up B's ancestors
            for (Family parentFamily : current.getParentFamilies(gedcom)) {
                for (Person parent : parentFamily.getHusbands(gedcom)) {
                    if (!visitedB.contains(parent.getId())) {
                        visitedB.add(parent.getId());
                        queueB.add(parent);
                        distancesB.add(distanceB + 1);
                    }
                }
                for (Person parent : parentFamily.getWives(gedcom)) {
                    if (!visitedB.contains(parent.getId())) {
                        visitedB.add(parent.getId());
                        queueB.add(parent);
                        distancesB.add(distanceB + 1);
                    }
                }
            }
        }
        
        return nearest;
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
     * Checks for Boru (Wife Taker) relationships in Batak Toba system
     */
    private String checkBoruRelationship(Person a, Person b) {
        // B is Boru to A if B's family received a wife from A's family
        
        // Check if B is husband of A's daughter
        for (Family childFamily : a.getSpouseFamilies(gedcom)) {
            for (Person child : childFamily.getChildren(gedcom)) {
                if (getGenderWithFallback(child) == Gender.FEMALE) {
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
                if (getGenderWithFallback(child) == Gender.FEMALE) {
                    for (Family grandchildFamily : child.getSpouseFamilies(gedcom)) {
                        for (Person grandchild : grandchildFamily.getChildren(gedcom)) {
                            if (grandchild.getId().equals(b.getId())) {
                                Gender grandchildGender = getGenderWithFallback(grandchild);
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
                            Gender siblingGender = getGenderWithFallback(motherSibling);
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
        Log.d("BatakKinship", "Sibling inheritance check for " + U.getPrincipalName(b)
                + " (target) relative to " + U.getPrincipalName(a));
        String relationFromSibling = checkSiblingInheritanceFromRelative(a, b);
        if (relationFromSibling != null) {
            return relationFromSibling;
        }

        return checkSiblingInheritanceFromRelative(b, a);
    }

    private String checkSiblingInheritanceFromRelative(Person relative, Person target) {
        Log.d("BatakKinship", "Checking siblings of " + U.getPrincipalName(relative)
                + " for relationship to " + U.getPrincipalName(target));
        // Get B's siblings
        List<Person> bSiblings = new ArrayList<>();
        for (Family parentFamily : relative.getParentFamilies(gedcom)) {
            List<String> childNames = new ArrayList<>();
            for (Person child : parentFamily.getChildren(gedcom)) {
                if (!child.getId().equals(relative.getId())) {
                    bSiblings.add(child);
                }
                childNames.add(U.getPrincipalName(child));
            }
            Log.d("BatakKinship", "Parent family " + parentFamily.getId()
                    + " children: " + childNames);
        }
        
        // Use sibling inheritance for affinal (marriage-based) relationships
        for (Person sibling : bSiblings) {
            if (areSiblings(sibling, target)) {
                Log.d("BatakKinship", "Skipping sibling " + U.getPrincipalName(sibling)
                        + " because they are a sibling of target " + U.getPrincipalName(target));
                continue;
            }
            Log.d("BatakKinship", "Checking sibling " + U.getPrincipalName(sibling)
                    + " for " + U.getPrincipalName(relative));
            List<Person> siblingPath = findShortestPath(sibling, target, 4);
            if (!siblingPath.isEmpty()) {
                boolean hasSpouseLink = pathIncludesSpouseLink(siblingPath);
                String siblingRelationship = analyzeBatakPathForRelationship(siblingPath);
                Log.d("BatakKinship", "Short path " + formatPathNames(siblingPath)
                        + " (len=" + siblingPath.size()
                        + ", spouseLink=" + hasSpouseLink
                        + ", rel=" + siblingRelationship + ")");
                if (siblingRelationship != null
                        && hasSpouseLink
                        && !siblingRelationship.equals(context.getString(R.string.rel_batak_non_relative))) {
                    Log.d("BatakKinship", "Sibling inheritance matched via short path: "
                            + siblingRelationship);
                    return siblingRelationship;
                }
                Log.d("BatakKinship", "Short path did not yield affinal match (len="
                        + siblingPath.size() + ", rel=" + siblingRelationship + ")");
            }

            List<Person> fallbackPath = findConnectionPathBFS(sibling, target);
            if (fallbackPath.isEmpty()) {
                Log.d("BatakKinship", "No BFS path to sibling " + U.getPrincipalName(sibling));
                continue;
            }

            boolean hasSpouseLink = pathIncludesSpouseLink(fallbackPath);
            String siblingRelationship = analyzeBatakPathForRelationship(fallbackPath);
            Log.d("BatakKinship", "BFS path " + formatPathNames(fallbackPath)
                    + " (len=" + fallbackPath.size()
                    + ", spouseLink=" + hasSpouseLink
                    + ", rel=" + siblingRelationship + ")");
            if (siblingRelationship == null) {
                Log.d("BatakKinship", "BFS path yielded no relationship for sibling "
                        + U.getPrincipalName(sibling));
                continue;
            }

            if (!hasSpouseLink) {
                Log.d("BatakKinship", "BFS path had no spouse link for sibling "
                        + U.getPrincipalName(sibling));
                continue;
            }

            if (siblingRelationship.equals(context.getString(R.string.rel_batak_non_relative))) {
                Log.d("BatakKinship", "BFS relationship is non-relative for sibling "
                        + U.getPrincipalName(sibling));
                continue;
            }
            Log.d("BatakKinship", "Sibling inheritance matched via BFS: " + siblingRelationship);
            return siblingRelationship;
        }
        Log.d("BatakKinship", "Sibling inheritance failed for " + U.getPrincipalName(relative));
        return null;
    }


    private String formatPathNames(List<Person> path) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                builder.append(" -> ");
            }
            builder.append(U.getPrincipalName(path.get(i)));
        }
        return builder.toString();
    }
    
    /**
     * Quick check if person B is known to be Amanguda to person A
     * without doing full BFS traversal
     */
    private boolean pathIncludesSpouseLink(List<Person> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            if (areSpouses(path.get(i), path.get(i + 1))) {
                return true;
            }
        }
        return false;
    }
}
