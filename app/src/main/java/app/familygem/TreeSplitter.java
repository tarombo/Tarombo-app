package app.familygem;

import com.familygem.utility.Helper;

import org.folg.gedcom.model.ChildRef;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;

import java.util.ArrayList;
import java.util.List;

public class TreeSplitter {
    private static class SplitterInfo {
        int generationsT1 = 1;
        int personsT1 = 1;
        int generationsT = 1;
        int personsT = 1;
        String subRepoUrl = "subRepoUrl";

    }
    public static class SplitterResult {
        Gedcom T1;
        int generationsT1 = 1;
        int personsT1 = 1;
    }

    // split tree T become T1 and T' where T = T1 + T'
    public static SplitterResult split(Gedcom gedcom, Settings.Tree treeGedcom, Person fulcrum,
                             String subRepoUrl) {
        SplitterInfo info = new SplitterInfo();
        info.generationsT = treeGedcom.generations;
        info.personsT = treeGedcom.persons;
        info.subRepoUrl = subRepoUrl;

        // create T1
        Gedcom T1 = new Gedcom();
        T1.setHeader(AlberoNuovo.creaTestata( "subtree"));
        T1.createIndexes();
        // clone person fulcrum and copy to T1
        Person clonedFulcrum = clonePerson(fulcrum);
        clonedFulcrum.setParentFamilyRefs(null); //remove family parent
        T1.addPerson(clonedFulcrum);
        getDescendants(fulcrum, gedcom, T1, info);
        // change fulcrum become CONNECTOR in T
        setPersonAsConnector(fulcrum, info);
        // re-indexing T
        gedcom.createIndexes();

        SplitterResult result = new SplitterResult();
        result.T1 = T1;
        result.personsT1 = info.personsT1;
        result.generationsT1 = info.generationsT1;
        treeGedcom.persons = gedcom.getPeople().size();
        // set root for T
        for (int i = 0; i < gedcom.getPeople().size(); i++) {
            if (!U.isConnector(gedcom.getPeople().get(i))) {
                treeGedcom.root = gedcom.getPeople().get(i).getId();
                break;
            }
        }
        Global.indi = treeGedcom.root;

//        int num = Global.settings.max() + 1;
//        result.treeT1 = new Settings.Tree(num, treeGedcom.title + " [subtree]", null, info.personsT1, info.generationsT1, fulcrum.getId(), null, 0, subRepoUrl);

        return result;
    }

    private static void getDescendants(Person p, Gedcom gedcom, Gedcom T1, SplitterInfo info) {
        List<Family> spouseFamilies = p.getSpouseFamilies(gedcom);
        for(Family spouseFamily : spouseFamilies) {
            System.out.println("spouse family id:" + spouseFamily.getId());
            // add spouse family to T1
            T1.addFamily(cloneFamily(spouseFamily));
            List<Person> wives = spouseFamily.getWives(gedcom);
            for (Person wive: wives) {
                if (!wive.getId().equals(p.getId())) {
                    // clone person wife and copy to T1
                    Person clonedWife = clonePerson(wive);
                    clonedWife.setParentFamilyRefs(null);
                    T1.addPerson(clonedWife);

                    // create connector on T2
                    System.out.println("CONNECTOR wive:" + getName(wive) + " id:" + wive.getId());
                    setPersonAsConnector(wive, info);

                    info.personsT1++;
                }
            }
            List<Person> husbands = spouseFamily.getHusbands(gedcom);
            for (Person husband: husbands) {
                if (!husband.getId().equals(p.getId())) {
                    // clone person husband and copy to T1
                    Person clonedHusband = clonePerson(husband);
                    clonedHusband.setParentFamilyRefs(null);
                    T1.addPerson(clonedHusband);

                    // change husband as connector on T2
                    System.out.println("CONNECTOR husband:" + getName(husband) + " id:" + husband.getId());
                    setPersonAsConnector(husband, info);

                    info.personsT1++;
                }
            }

            // process children
            List<Person> children = spouseFamily.getChildren(gedcom);
            if (children.size() > 0)
                info.generationsT1++;
            for (Person child : children) {
                System.out.println("child:" + getName(child) + " id:" + child.getId());
                // clone person child and copy to T1
                T1.addPerson(clonePerson(child));
                // recursively get next descendants
                getDescendants(child, gedcom, T1, info);
                info.personsT1++;
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

    private static void setPersonAsConnector(Person person, SplitterInfo info) {
        // Nome
        Name name = new Name();
        name.setValue("connector");
        List<Name> nomi = new ArrayList<>();
        nomi.add(name);
        person.setNames(nomi);

        // save URL of the sub repo (sub tree)
        EventFact connector = new EventFact();
        connector.setTag(U.CONNECTOR_TAG);
        connector.setValue(info.subRepoUrl);
        person.addEventFact(connector);
    }

    private static Person clonePerson(Person person) {
        Person clone = new Person();
        clone.setId(person.getId());
        clone.setParentFamilyRefs(person.getParentFamilyRefs());
        clone.setSpouseFamilyRefs(person.getSpouseFamilyRefs());
        clone.setNames(person.getNames());
        List<EventFact> eventFacts = new ArrayList<>();
        for (EventFact eventFact: person.getEventsFacts()) {
            eventFacts.add(cloneEventFact(eventFact));
        }
        clone.setEventsFacts(eventFacts);
        return clone;
    }

    private static EventFact cloneEventFact(EventFact eventFact) {
        EventFact clone = new EventFact();
        clone.setValue(eventFact.getValue());
        clone.setTag(eventFact.getTag());
        clone.setAddress(eventFact.getAddress());
        clone.setDate(eventFact.getDate());
        clone.setCause(eventFact.getCause());
        clone.setEmail(eventFact.getEmail());
        clone.setEmailTag(eventFact.getEmailTag());
        clone.setFax(eventFact.getFax());
        clone.setPhone(eventFact.getPhone());
        clone.setPlace(eventFact.getPlace());
        clone.setRin(eventFact.getRin());
        clone.setType(eventFact.getType());
        clone.setUid(eventFact.getUid());
        clone.setUidTag(eventFact.getUidTag());
        clone.setWww(eventFact.getWww());
        clone.setWwwTag(eventFact.getWwwTag());
        clone.setExtensions(eventFact.getExtensions());
        clone.setMedia(eventFact.getMedia());
        clone.setMediaRefs(eventFact.getMediaRefs());
        clone.setNoteRefs(eventFact.getNoteRefs());
        clone.setNotes(eventFact.getNotes());
        clone.setSourceCitations(eventFact.getSourceCitations());
        return clone;
    }

    private static Family cloneFamily(Family family) {
        Family clone = new Family();
        clone.setId(family.getId());
        List<ChildRef> childRefs = new ArrayList<>(family.getChildRefs());
        clone.setChildRefs(childRefs);
        clone.setHusbandRefs(family.getHusbandRefs());
        clone.setWifeRefs(family.getWifeRefs());
        return clone;
    }

    private static String getName(Person person) {
        for (Name name : person.getNames()) {
            return  name.getValue();
        }
        return "";
    }
}
