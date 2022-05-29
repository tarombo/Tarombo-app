package app.familygem;

import static org.junit.Assert.assertTrue;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Type;
import java.util.Map;

//@RunWith(AndroidJUnit4ClassRunner.class)
public class DiffTest {
    @Test
    public void compare1Test() {
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
