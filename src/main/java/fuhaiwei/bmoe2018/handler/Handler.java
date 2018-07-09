package fuhaiwei.bmoe2018.handler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static fuhaiwei.bmoe2018.autorun.RunTask.DATE_FORMATTER;
import static fuhaiwei.bmoe2018.autorun.RunTask.DATE_TIME_FORMATTER;
import static fuhaiwei.bmoe2018.utils.FileUtil.writeText;

public abstract class Handler {

    public static void handleData(JSONObject current, JSONArray data) {
        System.out.println("数据分析中...");

        Map<Integer, String> idToChnName = new LinkedHashMap<>();
        Map<Integer, String> idToGroupName = new HashMap<>();

        Map<Integer, Integer> idToVote = new HashMap<>();
        Map<Integer, Integer> idToLove = new HashMap<>();

        Map<String, Integer> groupPersonCount = new HashMap<>();
        Map<Integer, Integer> personVoteCount = new HashMap<>();

        JSONArray voteGroups = current.getJSONArray("voteGroups");

        for (int i = 0; i < voteGroups.length(); i++) {
            JSONObject group = voteGroups.getJSONObject(i);
            String groupName = group.getString("group_name");

            JSONArray characters = group.getJSONArray("characters");
            for (int j = 0; j < characters.length(); j++) {
                JSONObject character = characters.getJSONObject(j);
                Integer characterId = character.getInt("character_id");
                idToChnName.put(characterId, character.getString("chn_name"));
                idToGroupName.put(characterId, groupName);
            }
        }

        int totalPersonCount = data.length();
        System.out.println("投票统计中...");

        for (int i = 0; i < totalPersonCount; i++) {
            JSONObject vote = data.getJSONObject(i);
            String[] characterIds = vote.getString("character_ids").split(",");
            if (vote.getInt("type") == 1) {
                increment(personVoteCount, 0);
                Integer id = Integer.valueOf(characterIds[0]);
                increment(idToLove, id);
                increment(groupPersonCount, idToGroupName.get(id));
            } else {
                increment(personVoteCount, characterIds.length);
                for (String characterId : characterIds) {
                    Integer id = Integer.valueOf(characterId);
                    increment(idToVote, id);
                    increment(groupPersonCount, idToGroupName.get(id));
                }
            }
        }

        System.out.println("报表生成中...");

        StringBuilder builder = new StringBuilder();
        builder.append(current.getString("title"));
        appendQuote(builder, "本日总投票人数: " + totalPersonCount);
        builder.append("\n<br/>\n");

        Integer loveVoteCount = personVoteCount.get(0);
        builder.append("本日技术分析: \n");

        if (loveVoteCount != null) {
            builder.append("投真爱人数: ");
            builder.append(loveVoteCount);
            appendQuote(builder, percent(loveVoteCount, totalPersonCount));
            builder.append("\n");
        }

        for (int i = 0; i < voteGroups.length(); i++) {
            Integer countVoteCount = personVoteCount.get(i + 1);
            if (countVoteCount != null) {
                builder.append("投").append(i + 1).append("票人数: ").append(countVoteCount);
                appendQuote(builder, percent(countVoteCount, totalPersonCount));
                builder.append("\n");
            }
        }
        builder.append("<br/>\n");

        for (int i = 0; i < voteGroups.length(); i++) {
            JSONObject group = voteGroups.getJSONObject(i);
            String groupName = group.getString("group_name");
            Integer groupVoteCount = groupPersonCount.get(groupName);

            builder.append("====");
            builder.append(groupName);
            builder.append("====");
            if (groupVoteCount != null) {
                appendQuote(builder, "小组投票总人数: " + groupVoteCount + ", " + percent(groupVoteCount, totalPersonCount));
            }
            builder.append("\n");

            JSONArray characters = group.getJSONArray("characters");
            JSONObject[] array = new JSONObject[characters.length()];
            for (int j = 0; j < characters.length(); j++) {
                array[j] = characters.getJSONObject(j);
            }
            Arrays.sort(array, (o1, o2) -> {
                int finalVote1 = getFinalVote(idToLove, idToVote, o1.getInt("character_id"));
                int finalVote2 = getFinalVote(idToLove, idToVote, o2.getInt("character_id"));
                return finalVote2 - finalVote1;
            });

            int lastFinalVote = 0;
            for (int j = 0; j < array.length; j++) {
                Integer characterId = array[j].getInt("character_id");
                String chnName = array[j].getString("chn_name");
                builder.append(chnName);
                builder.append("\n");
                int voteCount = idToVote.getOrDefault(characterId, 0);
                int loveCount = idToLove.getOrDefault(characterId, 0);
                int finalCount = voteCount + loveCount * 2;
                builder.append("总票数: ").append(finalCount);
                if (j != 0) {
                    appendQuote(builder, "落后" + (lastFinalVote - finalCount) + "票");
                }
                builder.append("\n");
                builder.append("普通票: ").append(voteCount);
                builder.append("\n");
                builder.append("真爱票: ").append(loveCount);
                builder.append("\n<br/>\n");
                lastFinalVote = finalCount;
            }
        }

        String dateTimeText = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        writeText(builder.toString(), new File(String.format("output/%s.txt", dateTimeText)));

        Map<String, Integer> voteMap = new LinkedHashMap<>();

        Integer[] characterIds = idToChnName.keySet().toArray(new Integer[0]);
        for (int i = 0; i < characterIds.length; i++) {
            for (int j = i + 1; j < characterIds.length; j++) {
                int characterId1 = characterIds[i];
                int characterId2 = characterIds[j];
                for (int k = 0; k < totalPersonCount; k++) {
                    JSONObject vote = data.getJSONObject(k);
                    String[] split = vote.getString("character_ids").split(",");
                    if (vote.getInt("type") == 0) {
                        int voteCount = 0;
                        for (String characterId : split) {
                            int id = Integer.parseInt(characterId);
                            if (id == characterId1) {
                                voteCount++;
                            }
                            if (id == characterId2) {
                                voteCount++;
                            }
                        }
                        if (voteCount == 2) {
                            String key = characterId1 + "," + characterId2;
                            increment(voteMap, key);
                        }
                    }
                }
            }
        }

        String dateText = LocalDate.now().format(DATE_FORMATTER);

        StringBuilder builder2 = new StringBuilder();
        builder2.append("====连记票分析====");
        builder2.append("\n");
        voteMap.forEach((k, v) -> handleRow(idToChnName, builder2, k, v));
        writeText(builder2.toString(), new File(String.format("output/%s/连记票-分组顺序.txt", dateText)));

        Map<String, Integer> treeMap = new TreeMap<>((k1, k2) -> voteMap.get(k2) - voteMap.get(k1));
        treeMap.putAll(voteMap);

        StringBuilder builder3 = new StringBuilder();
        builder3.append("====连记票分析====");
        builder3.append("\n");
        treeMap.forEach((k, v) -> handleRow(idToChnName, builder3, k, v));
        writeText(builder3.toString(), new File(String.format("output/%s/连记票-票数顺序.txt", dateText)));
    }

    private static void handleRow(Map<Integer, String> nameMap, StringBuilder builder2, String k, Integer v) {
        String[] split = k.split(",");
        String name1 = nameMap.get(Integer.parseInt(split[0]));
        String name2 = nameMap.get(Integer.parseInt(split[1]));
        builder2.append(name1);
        builder2.append(" + ");
        builder2.append(name2);
        builder2.append(" = ");
        builder2.append(v);
        builder2.append("\n");
    }

    private static <T> void increment(Map<T, Integer> map, T key) {
        map.put(key, map.getOrDefault(key, 0) + 1);
    }

    private static String percent(double a, double b) {
        return String.format("%.2f%%", a / b * 100);
    }

    private static void appendQuote(StringBuilder builder, String string) {
        builder.append(" (");
        builder.append(string);
        builder.append(")");
    }

    private static int getFinalVote(Map<Integer, Integer> idToLove, Map<Integer, Integer> idToVote, Integer id) {
        return idToLove.getOrDefault(id, 0) * 2 + idToVote.getOrDefault(id, 0);
    }

}
