package app.familygem;

import androidx.annotation.NonNull;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import kotlin.Pair;


public class CompareDiffTree {
    public static class DiffPeople implements Serializable {
        String personId;
        Map<ChangeItem, Pair<String, String>> properties;
        ChangeType changeType;

        public DiffPeople(String personId, ChangeType changeType) {
            this.personId = personId;
            this.changeType = changeType;
            this.properties = new HashMap<>();

        }

        @NonNull
        @Override
        public String toString() {
            return "personId:" + personId + " changeType:" + changeType
                    + " properties:" + properties.keySet().stream().map(key -> key + "=" + properties.get(key)).collect(Collectors.joining(", ", "{", "}"));
        }
    }
    public enum ChangeType {
        NONE,
        ADDED,
        REMOVED,
        MODIFIED
    }

    public enum ChangeItem {
        NAME,
        SEX,
        BIRTH_DATE,
        BIRTH_PLACE,
        DEATH
    }


    public static List<DiffPeople> compare(Gedcom gedcomLeft, Gedcom gedcomRight )  {
        List<DiffPeople> diffPeopleList = new ArrayList<>();

        Map<String,Person> personIndexLeft = new HashMap<String, Person>();
        Map<String,Person> personIndexRight = new HashMap<String, Person>();
        for (Person person : gedcomLeft.getPeople()) {
            personIndexLeft.put(person.getId(), person);
        }
        for (Person person : gedcomRight.getPeople()) {
            personIndexRight.put(person.getId(), person);
        }

        // modified person
        Iterator<Map.Entry<String, Person>> iteratorLeft = personIndexLeft.entrySet().iterator();
        while (iteratorLeft.hasNext()) {
            Map.Entry<String, Person> entry = iteratorLeft.next();
            if (personIndexRight.get(entry.getKey()) != null) {
                DiffPeople diffPeople = new DiffPeople(entry.getKey(), ChangeType.NONE);
                if (isModified(entry.getValue(), personIndexRight.get(entry.getKey()), diffPeople)) {
                    diffPeople.changeType = ChangeType.MODIFIED;
                    diffPeopleList.add(diffPeople);
                }
            } else {
                // removed person
                DiffPeople diffPeople = new DiffPeople(entry.getKey(), ChangeType.REMOVED);
                diffPeople.properties.put(ChangeItem.NAME, new Pair<>(null, entry.getValue().getNames().stream()
                        .map(Name::getValue).collect(Collectors.joining(","))));
                diffPeopleList.add(diffPeople);
            }
            personIndexRight.remove(entry.getKey());
            iteratorLeft.remove();
        }

        // new person
        Iterator<Map.Entry<String, Person>> iteratorRight = personIndexRight.entrySet().iterator();
        while (iteratorRight.hasNext()) {
            Map.Entry<String, Person> entry = iteratorRight.next();
            DiffPeople diffPeople = new DiffPeople(entry.getKey(), ChangeType.ADDED);
            diffPeople.properties.put(ChangeItem.NAME, new Pair<>(null, entry.getValue().getNames().stream()
                    .map(Name::getValue).collect(Collectors.joining(","))));
            diffPeopleList.add(diffPeople);
            iteratorRight.remove();
        }

        System.out.println("remaining properties left only");
        personIndexLeft.forEach((key, value) -> System.out.println(key));

        System.out.println("remaining properties right only");
        personIndexRight.forEach((key, value) -> System.out.println(key));

        System.out.println("diffPeopleList");
        diffPeopleList.forEach(System.out::println);

        return diffPeopleList;
    }

    public static Boolean isModified(Person personLeft, Person personRight, DiffPeople diffPeople) {
        boolean isModified = false;

        // name
        String personNameLeft =
                personLeft.getNames().stream()
                        .map(Name::getValue).collect(Collectors.joining(","));
        String personNameRight =
                personRight.getNames().stream()
                        .map(Name::getValue).collect(Collectors.joining(","));
        if (!personNameLeft.equals(personNameRight)) {
            diffPeople.properties.put(ChangeItem.NAME, new Pair(personNameLeft, personNameRight));
            isModified = true;
        }

        // collect eventFact
        String personSexLeft = null;
        String personDeathLeft = null;
        String personBirthDateLeft = null;
        String personBirthPlaceLeft = null;
        for (EventFact eventFact :personLeft.getEventsFacts()) {
            if (eventFact.getTag().equals("SEX"))
                personSexLeft = eventFact.getValue();
            else if (eventFact.getTag().equals("DEAT"))
                personDeathLeft = eventFact.getValue();
            else if (eventFact.getTag().equals("BIRT")) {
                personBirthDateLeft = eventFact.getDate();
                personBirthPlaceLeft = eventFact.getPlace();
            }
        }
        String personSexRight = null;
        String personDeathRight = null;
        String personBirthDateRight = null;
        String personBirthPlaceRight = null;
        for (EventFact eventFact :personRight.getEventsFacts()) {
            if (eventFact.getTag().equals("SEX"))
                personSexRight = eventFact.getValue();
            else if (eventFact.getTag().equals("DEAT"))
                personDeathRight = eventFact.getValue();
            else if (eventFact.getTag().equals("BIRT")) {
                personBirthDateRight = eventFact.getDate();
                personBirthPlaceRight = eventFact.getPlace();
            }
        }

        if (isDifferent(personSexLeft, personSexRight))
            diffPeople.properties.put(ChangeItem.SEX, new Pair<>(personSexLeft, personSexRight));
        if (isDifferent(personDeathLeft, personDeathRight))
            diffPeople.properties.put(ChangeItem.DEATH, new Pair<>(personDeathLeft, personDeathRight));
        if (isDifferent(personBirthDateLeft, personBirthDateRight))
            diffPeople.properties.put(ChangeItem.BIRTH_DATE, new Pair<>(personBirthDateLeft, personBirthDateRight));
        if (isDifferent(personBirthPlaceLeft, personBirthPlaceRight))
            diffPeople.properties.put(ChangeItem.BIRTH_PLACE, new Pair<>(personBirthPlaceLeft, personBirthPlaceRight));

        return isModified;
    }

    private static Boolean isDifferent(String s1, String s2) {
        if (s1 == null && s2 == null)
            return false;
        if (s1 == null)
            return true;
        if (s2 == null)
            return true;
        return s1.equals(s2);
    }

}
