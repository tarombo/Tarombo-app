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
        assertEquals(2, gedcom.getFamilies().size());
        assertEquals(4, gedcom.getPeople().size());
        Optional<Person> person = gedcom.getPeople().stream().filter(x -> x.getId().equals("I1")).findFirst();
        assertTrue(person.isPresent());
    }
/*
    // scenario A - only modify property of one node
    @Test
    public void compareScenarioATest() throws IOException {
        String filenameLeft = "treeA_1.json";
        String leftJson = getJson(filenameLeft);
        String filenameRight = "treeA_2.json";
        String rightJson = getJson(filenameRight);

        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();

        Map<String, Object> leftMap = gson.fromJson(leftJson, type);
        Map<String, Object> rightMap = gson.fromJson(rightJson, type);

        Map<String, Object> leftFlatMap = FlatMapUtil.flatten(leftMap);
        Map<String, Object> rightFlatMap = FlatMapUtil.flatten(rightMap);

        MapDifference<String, Object> difference = Maps.difference(leftFlatMap, rightFlatMap);
//        System.out.println("Entries only on the left\n--------------------------");
//        difference.entriesOnlyOnLeft().forEach((key, value) -> System.out.println(key + ": " + value));
//        System.out.println("\n\nEntries only on the right\n--------------------------");
//        difference.entriesOnlyOnRight().forEach((key, value) -> System.out.println(key + ": " + value));
//        System.out.println("\n\nEntries differing\n--------------------------");
//        difference.entriesDiffering().forEach((key, value) -> System.out.println(key + ": " + value));
//        System.out.println("\n\nEntries common\n--------------------------");
//        difference.entriesInCommon().forEach((key, value) -> System.out.println(key + ": " + value));

        //only concern with people
        Map<String, Object> differenceOnlyLeft = difference.entriesOnlyOnLeft()
                .entrySet().stream().filter(x -> x.getKey().startsWith("/people"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        System.out.println("\n\nEntries only on the left for people\n--------------------------");
        differenceOnlyLeft.forEach((key, value) -> System.out.println(key + ": " + value));
        assertEquals(0, differenceOnlyLeft.size());

        System.out.println("\n\nEntries only on the right for people\n--------------------------");
        Map<String, Object> differenceOnlyRight = difference.entriesOnlyOnRight()
                .entrySet().stream().filter(x -> x.getKey().startsWith("/people"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        differenceOnlyRight.forEach((key, value) -> System.out.println(key + ": " + value));
        assertNotEquals(0, difference.entriesOnlyOnRight().size());

        System.out.println("\n\nEntries differing for people\n--------------------------");
        difference.entriesDiffering().forEach((key, value) ->
        {
            if (key.startsWith("/people")) {
                String[] properties = key.split("/");
                System.out.println(Arrays.toString(properties) + " -> " + value);
            }
        }
        );

        System.out.println("\n\nEntries common for people\n--------------------------");
        Map<String, Object> commons = difference.entriesInCommon()
                .entrySet().stream().filter(x -> x.getKey().startsWith("/people"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        commons.forEach((key, value) -> System.out.println(key + ": " + value));

        assertNotEquals(0, difference.entriesDiffering().size());
    }
*/
    // scenario A - only modify property of one node
    @Test
    public void compareScenarioATest2() throws IOException {
        String filenameLeft = "treeA_1.json";
        String leftJson = getJson(filenameLeft);
        String filenameRight = "treeA_2.json";
        String rightJson = getJson(filenameRight);

        List<CompareDiffTree.DiffPeople> diffPeopleList = CompareDiffTree.compare(leftJson, rightJson);
        assertEquals(1, diffPeopleList.size());
        assertEquals(CompareDiffTree.ChangeType.MODIFIED, diffPeopleList.get(0).changeType);
        assertEquals(4, diffPeopleList.get(0).properties.size());
        assertTrue(true);
    }
/*
    // scenario B - add a new node (people) --> there is entry /people/[]/id on the right
    @Test
    public void compareScenarioBTest() throws IOException {
        String filenameLeft = "treeA_2.json";
        String leftJson = getJson(filenameLeft);
        String filenameRight = "treeB_1.json";
        String rightJson = getJson(filenameRight);

        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();

//        System.out.println("leftJson:\n" + leftJson);
//        System.out.println("rightJson:\n" + rightJson);
        Map<String, Object> leftMap = gson.fromJson(leftJson, type);
        Map<String, Object> rightMap = gson.fromJson(rightJson, type);

        Map<String, Object> leftFlatMap = FlatMapUtil.flatten(leftMap);
        Map<String, Object> rightFlatMap = FlatMapUtil.flatten(rightMap);

        MapDifference<String, Object> difference = Maps.difference(leftFlatMap, rightFlatMap);
        System.out.println("Entries only on the left\n--------------------------");
        difference.entriesOnlyOnLeft().forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println("\n\nEntries only on the right\n--------------------------");
        difference.entriesOnlyOnRight().forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println("\n\nEntries differing\n--------------------------");
        difference.entriesDiffering().forEach((key, value) -> System.out.println(key + ": " + value));

        assertTrue(true);
    }
 */

    // scenario B - add a new node (people) --> there is entry /people/[]/id on the right
    @Test
    public void compareScenarioBTest2() throws IOException {
        String filenameLeft = "treeA_2.json";
        String leftJson = getJson(filenameLeft);
        String filenameRight = "treeB_1.json";
        String rightJson = getJson(filenameRight);

        List<CompareDiffTree.DiffPeople> diffPeopleList = CompareDiffTree.compare(leftJson, rightJson);
        assertEquals(1, diffPeopleList.size());
        assertEquals(CompareDiffTree.ChangeType.ADDED, diffPeopleList.get(0).changeType);
        assertEquals(3, diffPeopleList.get(0).properties.size());
    }

    /*
    // scenario C - delete a new node (people) --> there is entry /people/[]/id on the left
    @Test
    public void compareScenarioCTest() throws IOException {
        String filenameLeft = "treeB_1.json";
        String leftJson = getJson(filenameLeft);
        String filenameRight = "treeC_1.json";
        String rightJson = getJson(filenameRight);

        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();

//        System.out.println("leftJson:\n" + leftJson);
//        System.out.println("rightJson:\n" + rightJson);
        Map<String, Object> leftMap = gson.fromJson(leftJson, type);
        Map<String, Object> rightMap = gson.fromJson(rightJson, type);

        Map<String, Object> leftFlatMap = FlatMapUtil.flatten(leftMap);
        Map<String, Object> rightFlatMap = FlatMapUtil.flatten(rightMap);

        MapDifference<String, Object> difference = Maps.difference(leftFlatMap, rightFlatMap);
        System.out.println("Entries only on the left\n--------------------------");
        difference.entriesOnlyOnLeft().forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println("\n\nEntries only on the right\n--------------------------");
        difference.entriesOnlyOnRight().forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println("\n\nEntries differing\n--------------------------");
        difference.entriesDiffering().forEach((key, value) -> System.out.println(key + ": " + value));

        assertTrue(true);
    }
     */

    // scenario C - delete a new node (people) --> there is entry /people/[]/id on the left
    @Test
    public void compareScenarioCTest2() throws IOException {
        String filenameLeft = "treeB_1.json";
        String leftJson = getJson(filenameLeft);
        String filenameRight = "treeC_1.json";
        String rightJson = getJson(filenameRight);

        List<CompareDiffTree.DiffPeople> diffPeopleList = CompareDiffTree.compare(leftJson, rightJson);
        assertEquals(1, diffPeopleList.size());
        assertEquals(CompareDiffTree.ChangeType.REMOVED, diffPeopleList.get(0).changeType);
    }
}
