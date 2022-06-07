package app.familygem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.parser.JsonParser;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4ClassRunner.class)
public class DiffTest {
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
        String fileName = "treeA_1.json";
        String json = getJson(fileName);
        System.out.println(fileName);
        System.out.println(json);
        Gedcom gedcom = new JsonParser().fromJson(json);
        assertNotNull(gedcom.getHeader());
        assertEquals(0, gedcom.getFamilies().size());
        assertEquals(1, gedcom.getPeople().size());

        Optional<Person> person = gedcom.getPeople().stream().filter(x -> x.getId().startsWith("I1")).findFirst();
        assertTrue(person.isPresent());
    }

    // scenario A - only modify property of one node
    @Test
    public void compareScenarioATest() throws IOException {
        String filenameLeft = "treeA_1.json";
        String leftJson = getJson(filenameLeft);
        Gedcom gedcomLeft = new JsonParser().fromJson(leftJson);
        String filenameRight = "treeA_2.json";
        String rightJson = getJson(filenameRight);
        Gedcom gedcomRight = new JsonParser().fromJson(rightJson);

        List<CompareDiffTree.DiffPeople> diffPeopleList = CompareDiffTree.compare(gedcomLeft, gedcomRight);
        assertEquals(1, diffPeopleList.size());
        assertEquals(CompareDiffTree.ChangeType.MODIFIED, diffPeopleList.get(0).changeType);
        assertEquals(1, diffPeopleList.get(0).properties.size());
    }

    // scenario A - only modify property of one node
    @Test
    public void compareScenarioATest2() throws IOException {
        String filenameLeft = "treeA_1.json";
        String leftJson = getJson(filenameLeft);
        Gedcom gedcomLeft = new JsonParser().fromJson(leftJson);
        String filenameRight = "treeA_3.json";
        String rightJson = getJson(filenameRight);
        Gedcom gedcomRight = new JsonParser().fromJson(rightJson);

        List<CompareDiffTree.DiffPeople> diffPeopleList = CompareDiffTree.compare(gedcomLeft, gedcomRight);
        assertEquals(1, diffPeopleList.size());
        assertEquals(CompareDiffTree.ChangeType.MODIFIED, diffPeopleList.get(0).changeType);
        assertEquals(5, diffPeopleList.get(0).properties.size());
    }

    // scenario B - add a new node (people) --> there is entry /people/[]/id on the right
    @Test
    public void compareScenarioBTest() throws IOException {
        String filenameLeft = "treeA_2.json";
        String leftJson = getJson(filenameLeft);
        Gedcom gedcomLeft = new JsonParser().fromJson(leftJson);
        String filenameRight = "treeB_1.json";
        String rightJson = getJson(filenameRight);
        Gedcom gedcomRight = new JsonParser().fromJson(rightJson);

        List<CompareDiffTree.DiffPeople> diffPeopleList = CompareDiffTree.compare(gedcomLeft, gedcomRight);
        assertEquals(1, diffPeopleList.size());
        assertEquals(CompareDiffTree.ChangeType.ADDED, diffPeopleList.get(0).changeType);
        assertEquals(0, diffPeopleList.get(0).properties.size());
    }

    // scenario C - delete a new node (people) --> there is entry /people/[]/id on the left
    @Test
    public void compareScenarioCTest() throws IOException {
        String filenameLeft = "treeB_1.json";
        String leftJson = getJson(filenameLeft);
        Gedcom gedcomLeft = new JsonParser().fromJson(leftJson);
        String filenameRight = "treeC_1.json";
        String rightJson = getJson(filenameRight);
        Gedcom gedcomRight = new JsonParser().fromJson(rightJson);

        List<CompareDiffTree.DiffPeople> diffPeopleList = CompareDiffTree.compare(gedcomLeft, gedcomRight);
        assertEquals(1, diffPeopleList.size());
        assertEquals(CompareDiffTree.ChangeType.REMOVED, diffPeopleList.get(0).changeType);
        assertEquals(0, diffPeopleList.get(0).properties.size());
    }

    // scenario D - delete a node (I2), add a new node (I4), modify a node (I3)
    @Test
    public void compareScenarioDTest() throws IOException {
        String filenameLeft = "treeD_1.json";
        String leftJson = getJson(filenameLeft);
        Gedcom gedcomLeft = new JsonParser().fromJson(leftJson);
        String filenameRight = "treeD_2.json";
        String rightJson = getJson(filenameRight);
        Gedcom gedcomRight = new JsonParser().fromJson(rightJson);

        List<CompareDiffTree.DiffPeople> diffPeopleList = CompareDiffTree.compare(gedcomLeft, gedcomRight);
        assertEquals(3, diffPeopleList.size());
//        assertEquals(CompareDiffTree.ChangeType.REMOVED, diffPeopleList.get(0).changeType);
    }

    // scenario E - only rename title in one node
    @Test
    public void compareScenarioETest() throws IOException {
        String filenameLeft = "treeE_1.json";
        String leftJson = getJson(filenameLeft);
        Gedcom gedcomLeft = new JsonParser().fromJson(leftJson);
        String filenameRight = "treeE_2.json";
        String rightJson = getJson(filenameRight);
        Gedcom gedcomRight = new JsonParser().fromJson(rightJson);

        List<CompareDiffTree.DiffPeople> diffPeopleList = CompareDiffTree.compare(gedcomLeft, gedcomRight);
        assertEquals(1, diffPeopleList.size());
        assertEquals(CompareDiffTree.ChangeType.MODIFIED, diffPeopleList.get(0).changeType);
        assertEquals(1, diffPeopleList.get(0).properties.size());
    }
}
