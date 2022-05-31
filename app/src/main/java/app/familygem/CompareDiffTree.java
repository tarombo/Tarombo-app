package app.familygem;

import androidx.annotation.NonNull;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class CompareDiffTree {
    public static class DiffPeople {
        String personId;
        Map<String, Object> properties;
        ChangeType changeType;
        int tempIndexLeft; // element index array of json left
        int tempIndexRight; // element index array of json right
        public DiffPeople(String personId, ChangeType changeType, int tempIndexLeft, int tempIndexRight) {
            this.personId = personId;
            this.changeType = changeType;
            this.tempIndexRight = tempIndexRight;
            this.tempIndexLeft = tempIndexLeft;
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
        Iterator<Map.Entry<String, Object>> iterDifferenceOnlyRight = differenceOnlyRight.entrySet().iterator();
        while (iterDifferenceOnlyRight.hasNext()) {
            Map.Entry<String, Object> entry = iterDifferenceOnlyRight.next();
            String[] properties = entry.getKey().split("/");
            if (properties.length <= 3 || (!"names".equals(properties[3])
                    && !"eventsFacts".equals(properties[3]) && !"id".equals(properties[3]))) {
                // only concern "names" and "eventsFacts" (property of people)
                iterDifferenceOnlyRight.remove();
                continue;
            }
            if (properties.length == 4 && "id".equals(properties[3])) {
//                System.out.println("ADDED -> " + Arrays.toString(properties) + " -> " + entry.getValue());
                diffPeopleList.add(new DiffPeople(entry.getValue().toString(), ChangeType.ADDED, -1, Integer.parseInt(properties[2])));
                iterDifferenceOnlyRight.remove();
            }
        }
        // collect all properties of ADDED people
        iterDifferenceOnlyRight = differenceOnlyRight.entrySet().iterator();
        for (DiffPeople diffPeople :diffPeopleList) {
            while (iterDifferenceOnlyRight.hasNext()) {
                Map.Entry<String, Object> entry = iterDifferenceOnlyRight.next();
                String[] properties = entry.getKey().split("/");
                if (properties.length > 4 && diffPeople.tempIndexRight == Integer.parseInt(properties[2])) {
//                    System.out.println("ADDED -> " + Arrays.toString(properties) + " -> " + entry.getValue());
                    diffPeople.properties.put(entry.getKey(), entry.getValue());
                    iterDifferenceOnlyRight.remove();
                }
            }
        }

        // person is removed (only appear on left)
        Iterator<Map.Entry<String, Object>> iterDifferenceOnlyLeft = differenceOnlyLeft.entrySet().iterator();
        while (iterDifferenceOnlyLeft.hasNext()) {
            Map.Entry<String, Object> entry = iterDifferenceOnlyLeft.next();
            String[] properties = entry.getKey().split("/");
            if (properties.length <= 3 || (!"names".equals(properties[3])
                    && !"eventsFacts".equals(properties[3]) && !"id".equals(properties[3]))) {
                // only concern "names" and "eventsFacts" (property of people)
                iterDifferenceOnlyLeft.remove();
                continue;
            }
            if (properties.length == 4 && "id".equals(properties[3])) {
                System.out.println("REMOVED -> " + Arrays.toString(properties) + " -> " + entry.getValue());
                diffPeopleList.add(new DiffPeople(entry.getValue().toString(), ChangeType.REMOVED, Integer.parseInt(properties[2]), -1));
                iterDifferenceOnlyLeft.remove();
            }
        }
        // remove all properties of removed people
        iterDifferenceOnlyLeft = differenceOnlyLeft.entrySet().iterator();
        for (DiffPeople diffPeople :diffPeopleList) {
            if (diffPeople.changeType != ChangeType.REMOVED)
                continue;
            while (iterDifferenceOnlyLeft.hasNext()) {
                Map.Entry<String, Object> entry = iterDifferenceOnlyLeft.next();
                String[] properties = entry.getKey().split("/");
                if (properties.length > 4 && diffPeople.tempIndexLeft == Integer.parseInt(properties[2])) {
//                    System.out.println("REMOVED -> " + Arrays.toString(properties) + " -> " + entry.getValue());
                    iterDifferenceOnlyLeft.remove();
                }
            }
        }

        // person is modified (appear on left and right)
        Iterator<Map.Entry<String, Object>> iterDifferenceLeftRight = differenceLeftRight.entrySet().iterator();
        while (iterDifferenceLeftRight.hasNext()) {
            Map.Entry<String, Object> entry = iterDifferenceLeftRight.next();
            String[] properties = entry.getKey().split("/");
            if (properties.length <= 3 || (!"names".equals(properties[3])
                    && !"eventsFacts".equals(properties[3]) && !"id".equals(properties[3]))) {
                // only concern "names" and "eventsFacts" (property of people)
                iterDifferenceLeftRight.remove();
            }
        }

        // from here the remaining entry is only for people modification
        // TODO get personID of the modified people


        System.out.println("remaining properties right only");
        differenceOnlyRight.forEach((key, value) -> System.out.println(key + ": " + value));

        System.out.println("remaining properties left only");
        differenceOnlyLeft.forEach((key, value) -> System.out.println(key + ": " + value));

        System.out.println("remaining properties left right");
        differenceLeftRight.forEach((key, value) -> System.out.println(key + ": " + value));

        System.out.println("diffPeopleList");
        diffPeopleList.forEach(System.out::println);

        return diffPeopleList;
    }
}
