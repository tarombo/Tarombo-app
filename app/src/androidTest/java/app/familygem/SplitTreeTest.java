package app.familygem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static app.familygem.Global.gc;

import android.content.Context;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.platform.app.InstrumentationRegistry;

import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.parser.JsonParser;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4ClassRunner.class)
public class SplitTreeTest {
    Context testContext; // Contesto del test (per accedere alle risorse in /assets)

    private String getJson(String filename) throws IOException {
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
        InputStream inputStream = testContext.getAssets().open(filename);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        StringBuilder text = new StringBuilder();

        BufferedReader br = new BufferedReader(inputStreamReader);
        String line;
        while( (line = br.readLine()) != null ) {
            text.append(line);
            text.append('\n');
        }
        br.close();

        String json = text.toString();
        return json;
    }

    @Test
    public void convertJsonGedcom() throws IOException {
        String json = getJson("T_tree_v1.json");
        System.out.println(json);
        Gedcom gedcom = new JsonParser().fromJson(json);
        assertNotNull(gedcom.getHeader());
    }

    // T = T1 + T2
//    Gedcom T1;
//    Gedcom T2;

    private String subRepoUrl = "subRepoUrl";

    @Test
    public void fulcrumTest() throws  IOException {
        String json = getJson("T_tree_v1.json");
        Gedcom gedcom = new JsonParser().fromJson(json);
        String fulcrumId = "I5*5ba2f623-430c-4eef-ad41-1dff3c17218b";
        Person fulcrum = gedcom.getPerson(fulcrumId);
        System.out.println("fulcrum:" + getName(fulcrum) + " id:" + fulcrum.getId());

        // create T1
        Gedcom T1 = new Gedcom();
        T1.setHeader(AlberoNuovo.creaTestata("subtree"));
        T1.createIndexes();
        // clone person fulcrum and copy to T1
        T1.addPerson(clonePerson(fulcrum));
        getDescendants(fulcrum, gedcom, T1);
        // change fulcrum become CONNECTOR in T
        setPersonAsConnector(fulcrum);
        // re-indexing T
        gedcom.createIndexes();

        File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getDir("tmp1", Context.MODE_PRIVATE);
        if (!dir.exists())
            dir.mkdir();
        JsonParser jp = new JsonParser();

        // create T1.json
        String jsonT1 = jp.toJson(T1);
        File T1file = new File(dir, "T1.json");
        FileUtils.writeStringToFile(T1file, jsonT1, "UTF-8");

        // create remaining T.json
        String jsonT = jp.toJson(gedcom);
        File Tfile = new File(dir, "T.json");
        FileUtils.writeStringToFile(Tfile, jsonT, "UTF-8");


        assertEquals(12, gedcom.getPeople().size());
        assertEquals(9, T1.getPeople().size());
        assertEquals(5, gedcom.getFamilies().size());
        assertEquals(2, T1.getFamilies().size());
    }



    private void getDescendants(Person p, Gedcom gedcom, Gedcom T1) {
        List<Family> spouseFamilies = p.getSpouseFamilies(gedcom);
        for(Family spouseFamily : spouseFamilies) {
            System.out.println("spouse family id:" + spouseFamily.getId());
            // add spouse family to T1
            T1.addFamily(cloneFamily(spouseFamily));
            List<Person> wives = spouseFamily.getWives(gedcom);
            for (Person wive: wives) {
                if (!wive.getId().equals(p.getId())) {
                    // clone person wife and copy to T1
                    T1.addPerson(clonePerson(wive));

                    // create connector on T2
                    System.out.println("CONNECTOR wive:" + getName(wive) + " id:" + wive.getId());
                    setPersonAsConnector(wive);
                }
            }
            List<Person> husbands = spouseFamily.getHusbands(gedcom);
            for (Person husband: husbands) {
                if (!husband.getId().equals(p.getId())) {
                    // clone person husband and copy to T1
                    T1.addPerson(clonePerson(husband));

                    // change husband as connector on T2
                    System.out.println("CONNECTOR husband:" + getName(husband) + " id:" + husband.getId());
                    setPersonAsConnector(husband);
                }
            }

            // process children
            List<Person> children = spouseFamily.getChildren(gedcom);
            for (Person child : children) {
                System.out.println("child:" + getName(child) + " id:" + child.getId());
                // clone person child and copy to T1
                T1.addPerson(clonePerson(child));
                // recursively get next descendants
                getDescendants(child, gedcom, T1);
            }
            // remove from T
            for (Person child : children) {
                gedcom.getPeople().remove( child );
                for( Family f : child.getParentFamilies(gedcom) ) {	// scollega i suoi ref nelle famiglie
                    f.getChildRefs().remove( f.getChildren(gedcom).indexOf(child) );
                }
            }

        }
    }

    private void setPersonAsConnector(Person person) {
        // Nome
        Name name = new Name();
        name.setValue("connector");
        List<Name> nomi = new ArrayList<>();
        nomi.add(name);
        person.setNames(nomi);

        // save URL of the sub repo (sub tree)
        EventFact connector = new EventFact();
        connector.setTag(U.CONNECTOR_TAG);
        connector.setValue(subRepoUrl);
        person.addEventFact(connector);
    }

    private Person clonePerson(Person person) {
        Person clone = new Person();
        clone.setId(person.getId());
        clone.setParentFamilyRefs(person.getParentFamilyRefs());
        clone.setSpouseFamilyRefs(person.getSpouseFamilyRefs());
        clone.setNames(person.getNames());
        clone.setEventsFacts(person.getEventsFacts());
        return clone;
    }

    private Family cloneFamily(Family family) {
        Family clone = new Family();
        clone.setId(family.getId());
        clone.setChildRefs(family.getChildRefs());
        clone.setHusbandRefs(family.getHusbandRefs());
        clone.setWifeRefs(family.getWifeRefs());
        return clone;
    }


    private void printPersons(List<Person> persons) {
        for (Person person : persons) {
            System.out.println("personId:" + person.getId() + " name:" + getName(person));
        }
    }

    private String getName(Person person) {
        for (Name name : person.getNames()) {
            return  name.getValue();
        }
        return "";
    }



/*
    private Group createGroup(int generation, boolean mini, Util.Branch branch) {
        Group group = new Group(generation, mini, branch);


        return group;
    }

    private void marriageAndChildren(Person fulcrum, Gedcom gedcom) {
        Genus fulcrumGenus = this.findPersonGenus(fulcrum, gedcom);
        Iterator var5 = fulcrumGenus.iterator();

        while(var5.hasNext()) {
            Node node = (Node)var5.next();
            this.findDescendants(node, 0, this.descendantGenerations + 1, Util.Branch.NONE);
        }

    }

    private Genus findPersonGenus(Person person, Gedcom gedcom) {
        Genus genus = new Genus();
        // find familes of the spouse
        List<Family> families = person.getSpouseFamilies(gedcom);
        if (!families.isEmpty() ) {
            for(int i = 0; i < families.size(); ++i) {
                Family family = families.get(i);
                Node partnerNode = null;
                // TODO add all nodes
                Util.Match match = Util.Match.get(families.size(), side, i);
                Object partnerNode;
                switch(match) {
                    case SOLE:
                    case NEAR:
                        partnerNode = this.createNodeFromPerson(person, family, parentNode, generation, type, match);
                        break;
                    default:
                        partnerNode = this.createNextFamilyNode(family, person, generation, side, match);
                }

                if (group != null) {
                    group.addNode((Node)partnerNode);
                }

                genus.add(partnerNode);
            }
        } else {
            Node singleNode = this.createNodeFromPerson(person, (Family)null, parentNode, generation, type, Util.Match.SOLE);
            if (group != null) {
                group.addNode(singleNode);
            }

            genus.add(singleNode);
        }

        return genus;
    }

    private Node createNodeFromPerson(Person person, Family spouseFamily, Node parentNode, Gedcom gedcom) {
        PersonNode personNode = new PersonNode(gedcom, person, 1);
        personNode.generation = generation;
        personNode.origin = parentNode;
        personNode.match = match;
        if (type == Util.Card.FULCRUM) {
            this.fulcrumNode = personNode;
        }

        FamilyNode familyNode = null;
        if ((type == Util.Card.FULCRUM || type == Util.Card.REGULAR) && spouseFamily != null) {
            List<Person> spouses = this.getSpouses(spouseFamily);
            if (spouses.size() > 1 && this.withSpouses) {
                familyNode = new FamilyNode(spouseFamily, false, Util.Side.NONE);
                familyNode.generation = generation;
                familyNode.match = match;
                Iterator var10 = spouses.iterator();

                while(true) {
                    while(var10.hasNext()) {
                        Person spouse = (Person)var10.next();
                        if (spouse.equals(person) && !familyNode.partners.contains(personNode)) {
                            familyNode.addPartner(personNode);
                        } else {
                            PersonNode partnerNode = new PersonNode(this.gedcom, spouse, Util.Card.REGULAR);
                            partnerNode.generation = generation;
                            familyNode.addPartner(partnerNode);
                            this.findAcquiredAncestry(partnerNode);
                        }
                    }

                    familyNode.createBond();
                    break;
                }
            } else {
                personNode.spouseFamily = spouseFamily;
            }
        }

        if (familyNode != null) {

            return familyNode;
        } else {

            return personNode;
        }
    }
*/
}
