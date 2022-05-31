package app.familygem;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class CompareDiffTree {
    public static class DiffPeople {
        String personId;
        Map<String, Object> changes;
        ChangeType changeType;
        int tempIndexLeft; // element index array of json left
        int tempIndexRight; // element index array of json right
        public DiffPeople(String personId, ChangeType changeType, int tempIndexLeft, int tempIndexRight) {
            this.personId = personId;
            this.changeType = changeType;
            this.tempIndexRight = tempIndexRight;
            this.tempIndexLeft = tempIndexLeft;
        }
    }
    public enum ChangeType {
        NONE,
        ADDED,
        REMOVED,
        MODIFIED
    }

    public static List<DiffPeople> compare(String leftJson, String rightJson) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();

//        System.out.println("leftJson:\n" + leftJson);
//        System.out.println("rightJson:\n" + rightJson);
        Map<String, Object> leftMap = gson.fromJson(leftJson, type);
        Map<String, Object> rightMap = gson.fromJson(rightJson, type);

        Map<String, Object> leftFlatMap = FlatMapUtil.flatten(leftMap);
        Map<String, Object> rightFlatMap = FlatMapUtil.flatten(rightMap);
        MapDifference<String, Object> difference = Maps.difference(leftFlatMap, rightFlatMap);

        //only concern with people
        Map<String, Object> differenceOnlyLeft = difference.entriesOnlyOnLeft()
                .entrySet().stream().filter(x -> x.getKey().startsWith("/people"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, Object> differenceOnlyRight = difference.entriesOnlyOnRight()
                .entrySet().stream().filter(x -> x.getKey().startsWith("/people"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, Object> differenceLeftRight = difference.entriesDiffering()
                .entrySet().stream().filter(x -> x.getKey().startsWith("/people"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<DiffPeople> diffPeopleList = new ArrayList<>();

        // person is added (only appear on right)
        differenceOnlyRight.forEach((key, value) ->
            {
                String[] properties = key.split("/");
                if (properties.length == 4 && "id".equals(properties[3])) {
                    System.out.println("ADDED -> " + Arrays.toString(properties) + " -> " + value);
                    diffPeopleList.add(new DiffPeople(value.toString(), ChangeType.ADDED, -1, Integer.parseInt(properties[2])));
                    differenceOnlyRight.remove(key);
                }
            }
        );
        // TODO collect all properties of ADDED people

        // person is removed (only appear on left)
        differenceOnlyLeft.forEach((key, value) ->
            {
                String[] properties = key.split("/");
                if (properties.length == 4 && "id".equals(properties[3])) {
                    System.out.println("REMOVED -> " + Arrays.toString(properties) + " -> " + value);
                    diffPeopleList.add(new DiffPeople(value.toString(), ChangeType.REMOVED, Integer.parseInt(properties[2]), -1));
                    differenceOnlyRight.remove(key);
                }
            }
        );


        return diffPeopleList;
    }
}
