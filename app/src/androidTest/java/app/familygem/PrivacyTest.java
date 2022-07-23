package app.familygem;

import static org.junit.Assert.assertNotNull;

import static app.familygem.TreeSplitter.cloneEventFact;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.familygem.utility.Helper;
import com.familygem.utility.PrivatePerson;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.parser.GedcomTypeAdapter;
import org.folg.gedcom.parser.JsonParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PrivacyTest {
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
        String json = getJson("treeP.json");
        System.out.println("treeP.json");
        System.out.println(json);
        Gedcom gedcom = new JsonParser().fromJson(json);
        assertNotNull(gedcom.getHeader());
        gedcom.createIndexes();
        String personId = "I1*684d96e5-b24e-4684-be6f-eb6f9626de6e";
        Person person =  gedcom.getPerson(personId);
        Assert.assertEquals(personId, person.getId());
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Gedcom.class, new GedcomTypeAdapter())
                .create();
        String personJsonStr = gson.toJson(person);
        System.out.println("personId:" + personId);
        System.out.println(personJsonStr);


        Person clone = new Person();
        clone.setId(personId);
        List<Media> mediaList = person.getAllMedia(gedcom);
        for (Media media: mediaList) {
            clone.addMedia(media);
        }
        List<EventFact> eventFacts = new ArrayList<>();
        for (EventFact eventFact: person.getEventsFacts()) {
            eventFacts.add(cloneEventFact(eventFact));
        }
        clone.setEventsFacts(eventFacts);
        String cloneJsonStr = gson.toJson(clone);
        System.out.println("personId:" + clone.getId());
        System.out.println(cloneJsonStr);

    }

    @Test
    public void setPrivateTest() throws IOException {
        String json = getJson("treeP.json");
        Gedcom gedcom = new JsonParser().fromJson(json);
        gedcom.createIndexes();
        String personId = "I1*684d96e5-b24e-4684-be6f-eb6f9626de6e";

        Person person =  gedcom.getPerson(personId);
        Assert.assertEquals(personId, person.getId());
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Gedcom.class, new GedcomTypeAdapter())
                .create();
        PrivatePerson privatePerson = setPrivate(gedcom, person);
        Assert.assertEquals(personId, privatePerson.personId);
        Assert.assertNotNull(privatePerson.eventFacts);
        Assert.assertNotNull(privatePerson.mediaList);
        Assert.assertEquals(1, person.getEventsFacts().size());
        Assert.assertEquals(0, person.getMedia().size());
        Assert.assertTrue(U.isPrivate(person));

        String privateJsonStr = gson.toJson(privatePerson);
        String personJsonStr = gson.toJson(person);
        System.out.println("private -------");
        System.out.println(privateJsonStr);
        System.out.println("person -------");
        System.out.println(personJsonStr);

        File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getDir("tmp1", Context.MODE_PRIVATE);
        if (!dir.exists())
            dir.mkdir();

        // save private.json
        File privateFile = new File(dir, "private.json");
        FileUtils.writeStringToFile(privateFile, privateJsonStr, "UTF-8");
        System.out.println("privateFile:" + privateFile.getAbsolutePath());
        // save person.json
        File personFile = new File(dir, "person.json");
        FileUtils.writeStringToFile(personFile, personJsonStr, "UTF-8");
    }

    // test json --> contains node father, mother, child
    // and then set father as private
    // how to set person as private
    // 1. clone the person
    // 2. add tag = PRIVATE
    // 3. on current person, clear all fields or create new person with same ID

    // return new but cloned person (same properties including personId)
    public static PrivatePerson setPrivate(Gedcom gedcom, Person person) {
        // clone person
        PrivatePerson clone = new PrivatePerson();
        clone.personId = person.getId();
        clone.mediaList = new ArrayList<>();
        List<Media> mediaList = person.getAllMedia(gedcom);
        for (Media media: mediaList) {
            clone.mediaList.add(media);
        }
        List<EventFact> eventFacts = new ArrayList<>();
        for (EventFact eventFact: person.getEventsFacts()) {
            eventFacts.add(cloneEventFact(eventFact));
        }
        clone.eventFacts = eventFacts;

        // clear all fields of the person (except names)
        person.setEventsFacts(new ArrayList<>());
        person.setMedia(new ArrayList<>());
        // add tag
        EventFact privacy = new EventFact();
        privacy.setTag(U.PRIVATE_TAG);
        privacy.setValue("");
        person.addEventFact(privacy);


        return clone;
    }


}
