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
        int tempIndex; // element index array of json
        public DiffPeople(String personId, ChangeType changeType, int tempIndex) {
            this.personId = personId;
            this.changeType = changeType;
            this.tempIndex = tempIndex;
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
        Map<String, Object> commons = difference.entriesInCommon()
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
                System.out.println("ADDED -> " + Arrays.toString(properties) + " -> " + entry.getValue());
                diffPeopleList.add(new DiffPeople(entry.getValue().toString(), ChangeType.ADDED, Integer.parseInt(properties[2])));
                iterDifferenceOnlyRight.remove();
            }
        }
        // collect all properties of ADDED people
        iterDifferenceOnlyRight = differenceOnlyRight.entrySet().iterator();
        for (DiffPeople diffPeople :diffPeopleList) {
            while (iterDifferenceOnlyRight.hasNext()) {
                Map.Entry<String, Object> entry = iterDifferenceOnlyRight.next();
                String[] properties = entry.getKey().split("/");
                if (properties.length > 4 && diffPeople.tempIndex == Integer.parseInt(properties[2])) {
                    System.out.println("ADDED -> " + Arrays.toString(properties) + " -> " + entry.getValue());
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
                diffPeopleList.add(new DiffPeople(entry.getValue().toString(), ChangeType.REMOVED, Integer.parseInt(properties[2])));
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
                if (properties.length > 4 && diffPeople.tempIndex == Integer.parseInt(properties[2])) {
                    System.out.println("REMOVED -> " + Arrays.toString(properties) + " -> " + entry.getValue());
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

        // get personID of the possible modified people
        Iterator<Map.Entry<String, Object>> iterator = commons.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            String[] properties = entry.getKey().split("/");
//            System.out.println("COMMON -> " + Arrays.toString(properties) + " -> " + entry.getValue());
            if (properties.length == 4 && "people".equals(properties[1]) && "id".equals(properties[3])) {
                boolean isModifiedPeople = true;
                int index = Integer.parseInt(properties[2]);
                for (DiffPeople diffPeople : diffPeopleList) {
                    if (diffPeople.tempIndex == index) {
                        isModifiedPeople = false;
                        break;
                    }
                }
                if (isModifiedPeople) {
                    diffPeopleList.add(new DiffPeople(entry.getValue().toString(), ChangeType.NONE, index));
                }
            }
        }


        // from here the remaining entry is only for people modification
        for (DiffPeople diffPeople :diffPeopleList) {
            if (diffPeople.changeType == ChangeType.ADDED || diffPeople.changeType == ChangeType.REMOVED)
                continue;

            // right only appear
            iterDifferenceOnlyRight = differenceOnlyRight.entrySet().iterator();
            while (iterDifferenceOnlyRight.hasNext()) {
                Map.Entry<String, Object> entry = iterDifferenceOnlyRight.next();
                String[] properties = entry.getKey().split("/");
                if (properties.length > 4 && diffPeople.tempIndex == Integer.parseInt(properties[2])) {
                    System.out.println("MODIFIED -> " + Arrays.toString(properties) + " -> " + entry.getValue());
                    diffPeople.properties.put(entry.getKey(), entry.getValue());
                    iterDifferenceOnlyRight.remove();
                    diffPeople.changeType = ChangeType.MODIFIED;
                }
            }

            // left only appear
            iterDifferenceOnlyLeft = differenceOnlyLeft.entrySet().iterator();
            while (iterDifferenceOnlyLeft.hasNext()) {
                Map.Entry<String, Object> entry = iterDifferenceOnlyLeft.next();
                String[] properties = entry.getKey().split("/");
                if (properties.length > 4 && diffPeople.tempIndex == Integer.parseInt(properties[2])) {
                    System.out.println("MODIFIED -> " + Arrays.toString(properties) + " -> " + entry.getValue());
                    diffPeople.properties.put(entry.getKey(), entry.getValue());
                    iterDifferenceOnlyLeft.remove();
                    diffPeople.changeType = ChangeType.MODIFIED;
                }
            }

            // both left and right appear
            iterDifferenceLeftRight = differenceLeftRight.entrySet().iterator();
            while (iterDifferenceLeftRight.hasNext()) {
                Map.Entry<String, Object> entry = iterDifferenceLeftRight.next();
                String[] properties = entry.getKey().split("/");
                if (properties.length > 4 && diffPeople.tempIndex == Integer.parseInt(properties[2])) {
                    System.out.println("MODIFIED -> " + Arrays.toString(properties) + " -> " + entry.getValue());
                    diffPeople.properties.put(entry.getKey(), entry.getValue());
                    iterDifferenceLeftRight.remove();
                    diffPeople.changeType = ChangeType.MODIFIED;
                }
            }
        }

        // remove NONE from diffPeopleList
        diffPeopleList.removeIf(x -> x.changeType == ChangeType.NONE);

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

    public static String getPeopleId(Map<String, Object> commons, int index) {
        Iterator<Map.Entry<String, Object>> iterator = commons.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            String[] properties = entry.getKey().split("/");
            if (properties.length == 4 && "people".equals(properties[1]) && "id".equals(properties[3])) {
                if (index == Integer.parseInt(properties[2]))
                    return entry.getValue().toString();
            }
        }
        return null;
    }
}
