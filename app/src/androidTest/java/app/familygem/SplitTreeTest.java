package app.familygem;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.platform.app.InstrumentationRegistry;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.parser.JsonParser;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    @Test
    public void fulcrumTest() throws  IOException {
        String json = getJson("T_tree_v1.json");
        Gedcom gedcom = new JsonParser().fromJson(json);
        String fulcrumId = "I5*5ba2f623-430c-4eef-ad41-1dff3c17218b";
        Person fulcrum = gedcom.getPerson(fulcrumId);
        for (Name name : fulcrum.getNames()) {
            System.out.println("name:" + name.getValue());
        }


        List<Family> spouseFamilies = fulcrum.getSpouseFamilies(gedcom);
        for(Family spouseFamily : spouseFamilies) {
            // TODO: determine if fulcrum is wive or husband or none
            System.out.println("spouse family id:" + spouseFamily.getId());
            List<Person> wives = spouseFamily.getWives(gedcom);
            System.out.println("wives ---");
            printPersons(wives);
            List<Person> husbands = spouseFamily.getHusbands(gedcom);
            System.out.println("husbands ---");
            printPersons(husbands);
            List<Person> children = spouseFamily.getChildren(gedcom);
            System.out.println("children ---");
            printPersons(children); // --> get their spouse families and then their children
        }

        List<Family> parentFamilies = fulcrum.getParentFamilies(gedcom);
        for(Family family : parentFamilies) {
            System.out.println("parent family id:" + family.getId());
        }



//        Genus fulcrumGenus = this.findPersonGenus(fulcrum, gedcom);
//        fulcrumGroup = this.createGroup(0, false, Util.Branch.NONE);
//        this.marriageAndChildren(fulcrum, (Node)null, this.fulcrumGroup);

        assertTrue(true);
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
