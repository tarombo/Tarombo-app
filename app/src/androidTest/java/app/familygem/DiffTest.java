package app.familygem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.widget.Toast;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.parser.JsonParser;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
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
    }

    // scenario A - only modify property of one node
    @Test
    public void compareScenarioATest() throws IOException {
        String filenameLeft = "treeA_1.json";
        String leftJson = getJson(filenameLeft);
        String filenameRight = "treeA_2.json";
        String rightJson = getJson(filenameRight);

        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();

//        System.out.println("leftJson:\n" + leftJson);
//        System.out.println("rightJson:\n" + rightJson);
        Map<String, Object> leftMap = gson.fromJson(leftJson, type);
        Map<String, Object> rightMap = gson.fromJson(rightJson, type);

        Map<String, Object> leftFlatMap = FlatMapUtil.flatten(leftMap);
        Map<String, Object> rightFlatMap = FlatMapUtil.flatten(rightMap);
//        System.out.println("leftMap:");
//        leftFlatMap.forEach((key, value) -> System.out.println(key + ": " + value));
//        System.out.println("rightMap:");
//        rightMap.forEach((key, value) -> System.out.println(key + ": " + value));

        MapDifference<String, Object> difference = Maps.difference(leftFlatMap, rightFlatMap);
        System.out.println("Entries only on the left\n--------------------------");
        difference.entriesOnlyOnLeft().forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println("\n\nEntries only on the right\n--------------------------");
        difference.entriesOnlyOnRight().forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println("\n\nEntries differing\n--------------------------");
        difference.entriesDiffering().forEach((key, value) -> System.out.println(key + ": " + value));

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
                System.out.println(key + ": " + Arrays.toString(properties));
            }
        }
        );
        assertNotEquals(0, difference.entriesDiffering().size());
    }

    @Test
    public void compare1Test() throws IOException {
//        File json1 = new File(
//                getClass().getResource("/test.json").getPath());
//        System.out.println("file: " + json1.exists());
        // see https://stackoverflow.com/a/50969020
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();

        String leftJson = "{\n" +
                "  \"families\": [\n" +
                "    {\n" +
                "      \"childRefs\": [\n" +
                "        {\n" +
                "          \"ref\": \"I3\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"husbandRefs\": [\n" +
                "        {\n" +
                "          \"ref\": \"I1\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"F1\",\n" +
                "      \"wifeRefs\": [\n" +
                "        {\n" +
                "          \"ref\": \"I2\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"chan\": {\n" +
                "        \"date\": {\n" +
                "          \"time\": \"20:19:37\",\n" +
                "          \"value\": \"29 MAY 2022\"\n" +
                "        },\n" +
                "        \"extensions\": {\n" +
                "          \"zone\": \"Asia/Tokyo\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"head\": {\n" +
                "    \"charset\": {\n" +
                "      \"value\": \"UTF-8\"\n" +
                "    },\n" +
                "    \"date\": {\n" +
                "      \"time\": \"20:19:37\",\n" +
                "      \"value\": \"29 MAY 2022\"\n" +
                "    },\n" +
                "    \"file\": \"2.json\",\n" +
                "    \"gedc\": {\n" +
                "      \"form\": \"LINEAGE-LINKED\",\n" +
                "      \"vers\": \"5.5.1\"\n" +
                "    },\n" +
                "    \"lang\": \"English\",\n" +
                "    \"sour\": {\n" +
                "      \"name\": \"Family Gem\",\n" +
                "      \"value\": \"FAMILY_GEM\",\n" +
                "      \"vers\": \"0.1.0\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"people\": [\n" +
                "    {\n" +
                "      \"fams\": [\n" +
                "        {\n" +
                "          \"ref\": \"F1\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"I1\",\n" +
                "      \"names\": [\n" +
                "        {\n" +
                "          \"value\": \"papa //\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"chan\": {\n" +
                "        \"date\": {\n" +
                "          \"time\": \"22:49:02\",\n" +
                "          \"value\": \"22 MAY 2022\"\n" +
                "        },\n" +
                "        \"extensions\": {\n" +
                "          \"zone\": \"Asia/Tokyo\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"extensions\": {\n" +
                "        \"kin\": 1\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"fams\": [\n" +
                "        {\n" +
                "          \"ref\": \"F1\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"I2\",\n" +
                "      \"names\": [\n" +
                "        {\n" +
                "          \"value\": \"mama xy //\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"chan\": {\n" +
                "        \"date\": {\n" +
                "          \"time\": \"20:04:36\",\n" +
                "          \"value\": \"29 MAY 2022\"\n" +
                "        },\n" +
                "        \"extensions\": {\n" +
                "          \"zone\": \"Asia/Tokyo\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"eventsFacts\": [\n" +
                "        {\n" +
                "          \"tag\": \"SEX\",\n" +
                "          \"value\": \"F\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"extensions\": {\n" +
                "        \"kin\": 1\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"famc\": [\n" +
                "        {\n" +
                "          \"ref\": \"F1\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"I3\",\n" +
                "      \"names\": [\n" +
                "        {\n" +
                "          \"value\": \"dddee //\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"chan\": {\n" +
                "        \"date\": {\n" +
                "          \"time\": \"20:19:37\",\n" +
                "          \"value\": \"29 MAY 2022\"\n" +
                "        },\n" +
                "        \"extensions\": {\n" +
                "          \"zone\": \"Asia/Tokyo\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        String rightJson = "{\n" +
                "  \"families\": [\n" +
                "    {\n" +
                "      \"childRefs\": [\n" +
                "        {\n" +
                "          \"ref\": \"I3\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"husbandRefs\": [\n" +
                "        {\n" +
                "          \"ref\": \"I1\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"F1\",\n" +
                "      \"wifeRefs\": [\n" +
                "        {\n" +
                "          \"ref\": \"I2\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"chan\": {\n" +
                "        \"date\": {\n" +
                "          \"time\": \"20:19:37\",\n" +
                "          \"value\": \"29 MAY 2022\"\n" +
                "        },\n" +
                "        \"extensions\": {\n" +
                "          \"zone\": \"Asia/Tokyo\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"childRefs\": [\n" +
                "        {\n" +
                "          \"ref\": \"I1\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"husbandRefs\": [\n" +
                "        {\n" +
                "          \"ref\": \"I4\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"F2\",\n" +
                "      \"chan\": {\n" +
                "        \"date\": {\n" +
                "          \"time\": \"20:30:09\",\n" +
                "          \"value\": \"29 MAY 2022\"\n" +
                "        },\n" +
                "        \"extensions\": {\n" +
                "          \"zone\": \"Asia/Tokyo\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"head\": {\n" +
                "    \"charset\": {\n" +
                "      \"value\": \"UTF-8\"\n" +
                "    },\n" +
                "    \"date\": {\n" +
                "      \"time\": \"20:30:09\",\n" +
                "      \"value\": \"29 MAY 2022\"\n" +
                "    },\n" +
                "    \"file\": \"2.json\",\n" +
                "    \"gedc\": {\n" +
                "      \"form\": \"LINEAGE-LINKED\",\n" +
                "      \"vers\": \"5.5.1\"\n" +
                "    },\n" +
                "    \"lang\": \"English\",\n" +
                "    \"sour\": {\n" +
                "      \"name\": \"Family Gem\",\n" +
                "      \"value\": \"FAMILY_GEM\",\n" +
                "      \"vers\": \"0.1.0\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"people\": [\n" +
                "    {\n" +
                "      \"famc\": [\n" +
                "        {\n" +
                "          \"ref\": \"F2\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"fams\": [\n" +
                "        {\n" +
                "          \"ref\": \"F1\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"I1\",\n" +
                "      \"names\": [\n" +
                "        {\n" +
                "          \"value\": \"papa //\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"chan\": {\n" +
                "        \"date\": {\n" +
                "          \"time\": \"20:30:09\",\n" +
                "          \"value\": \"29 MAY 2022\"\n" +
                "        },\n" +
                "        \"extensions\": {\n" +
                "          \"zone\": \"Asia/Tokyo\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"extensions\": {\n" +
                "        \"kin\": 2\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"fams\": [\n" +
                "        {\n" +
                "          \"ref\": \"F1\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"I2\",\n" +
                "      \"names\": [\n" +
                "        {\n" +
                "          \"value\": \"mama xy //\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"chan\": {\n" +
                "        \"date\": {\n" +
                "          \"time\": \"20:04:36\",\n" +
                "          \"value\": \"29 MAY 2022\"\n" +
                "        },\n" +
                "        \"extensions\": {\n" +
                "          \"zone\": \"Asia/Tokyo\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"eventsFacts\": [\n" +
                "        {\n" +
                "          \"tag\": \"SEX\",\n" +
                "          \"value\": \"F\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"extensions\": {\n" +
                "        \"kin\": 1\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"famc\": [\n" +
                "        {\n" +
                "          \"ref\": \"F1\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"I3\",\n" +
                "      \"names\": [\n" +
                "        {\n" +
                "          \"value\": \"dddee //\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"chan\": {\n" +
                "        \"date\": {\n" +
                "          \"time\": \"20:19:37\",\n" +
                "          \"value\": \"29 MAY 2022\"\n" +
                "        },\n" +
                "        \"extensions\": {\n" +
                "          \"zone\": \"Asia/Tokyo\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"fams\": [\n" +
                "        {\n" +
                "          \"ref\": \"F2\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"I4\",\n" +
                "      \"names\": [\n" +
                "        {\n" +
                "          \"value\": \"grand ccc //\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"chan\": {\n" +
                "        \"date\": {\n" +
                "          \"time\": \"20:30:09\",\n" +
                "          \"value\": \"29 MAY 2022\"\n" +
                "        },\n" +
                "        \"extensions\": {\n" +
                "          \"zone\": \"Asia/Tokyo\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"eventsFacts\": [\n" +
                "        {\n" +
                "          \"tag\": \"SEX\",\n" +
                "          \"value\": \"M\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        System.out.println("leftJson:\n" + leftJson);
        System.out.println("rightJson:\n" + rightJson);
        Map<String, Object> leftMap = gson.fromJson(leftJson, type);
        Map<String, Object> rightMap = gson.fromJson(rightJson, type);

        Map<String, Object> leftFlatMap = FlatMapUtil.flatten(leftMap);
        Map<String, Object> rightFlatMap = FlatMapUtil.flatten(rightMap);

        MapDifference<String, Object> difference = Maps.difference(leftFlatMap, rightFlatMap);
        System.out.println("Entries only on the left\n--------------------------");
        difference.entriesOnlyOnLeft()
                .forEach((key, value) -> System.out.println(key + ": " + value));

        System.out.println("\n\nEntries only on the right\n--------------------------");
        difference.entriesOnlyOnRight()
                .forEach((key, value) -> System.out.println(key + ": " + value));

        System.out.println("\n\nEntries differing\n--------------------------");
        difference.entriesDiffering()
                .forEach((key, value) -> System.out.println(key + ": " + value));

        assertTrue(true);
    }
}
