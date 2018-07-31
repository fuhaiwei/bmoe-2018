package fuhaiwei.bmoe2018.handler;

import fuhaiwei.bmoe2018.utils.Permutations;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fuhaiwei.bmoe2018.spider.BmoeSpider.fetchGroup;

public abstract class Handler {

    public static HandlerResult handleData(JSONObject current, JSONArray data, boolean checkUnion) {
        JSONArray voteGroups = current.getJSONArray("voteGroups");

        int groupCount = voteGroups.length();
        int dataCount = data.length();

        Map<Integer, String> idToChnName = new LinkedHashMap<>();
        Map<Integer, String> idToGroupName = new HashMap<>();

        Map<Integer, Integer> idToVote = new HashMap<>();
        Map<Integer, Integer> idToLove = new HashMap<>();

        Map<String, Integer> groupNameToVote = new HashMap<>();
        Map<Integer, Integer> voteCountCount = new HashMap<>();
        Map<Integer, Map<String, Integer>> unionVoteData = new HashMap<>();

        System.out.println("数据分析中...");

        for (int i = 0; i < groupCount; i++) {
            JSONObject group = voteGroups.getJSONObject(i);
            String groupName = group.getString("group_name");

            JSONArray characters = group.getJSONArray("characters");
            for (int j = 0; j < characters.length(); j++) {
                JSONObject character = characters.getJSONObject(j);
                Integer characterId = character.getInt("character_id");
                idToChnName.put(characterId, character.getString("chn_name"));
                idToGroupName.put(characterId, groupName);
            }

            if (i > 0) {
                unionVoteData.put(i + 1, new HashMap<>());
            }
        }

        System.out.println("投票统计中...");

        for (int i = 0; i < dataCount; i++) {
            JSONObject vote = data.getJSONObject(i);
            String[] characterIds = vote.getString("character_ids").split(",");
            if (vote.getInt("type") == 1) {
                increment(voteCountCount, 0);
                Integer id = Integer.valueOf(characterIds[0]);
                increment(idToLove, id);
                increment(groupNameToVote, idToGroupName.get(id));
            } else {
                int voteCount = characterIds.length;
                increment(voteCountCount, voteCount);
                for (String characterId : characterIds) {
                    Integer id = Integer.valueOf(characterId);
                    increment(idToVote, id);
                    increment(groupNameToVote, idToGroupName.get(id));
                }

                if (checkUnion && voteCount > 1) {
                    for (int len = 2; len <= voteCount; len++) {
                        Map<String, Integer> map = unionVoteData.get(len);
                        new Permutations<>(characterIds, new String[len])
                                .forEach(result -> increment(map, String.join(",", result)));
                    }
                }
            }
        }

        System.out.println("报表生成中...");

        StringBuilder builder = new StringBuilder();

        appendHeader(builder, current, dataCount);
        appendVoteData(builder, voteCountCount, groupCount, dataCount);

        for (int i = 0; i < groupCount; i++) {
            JSONObject group = voteGroups.getJSONObject(i);
            String groupName = group.getString("group_name");
            Integer groupVoteCount = groupNameToVote.get(groupName);

            builder.append("====");
            builder.append(groupName);
            builder.append("====");
            if (groupVoteCount != null) {
                String groupVoteData = String.format("小组投票总人数: %d, %s",
                        groupVoteCount, percent(groupVoteCount, dataCount));
                appendQuote(builder, groupVoteData);
            }
            builder.append("\n");

            fetchGroup(group.getInt("group_id"), root -> {
                JSONArray voteInfo = root.getJSONArray("result");
                boolean hasPrev = root.has("prevResult");

                int prevSum = 0;
                int prevNum = 0;

                for (int j = 0; j < voteInfo.length(); j++) {
                    JSONObject thisChn = voteInfo.getJSONObject(j);

                    int thisSum = thisChn.getInt("ballot_sum");
                    int thisNum = thisChn.getInt("ballot_num");
                    int thisRatio = thisChn.getInt("ballot_ratio");
                    int thisChnId = thisChn.getInt("character_id");
                    int loveCount = safeGet(idToLove, thisChnId);

                    builder.append(j + 1);
                    builder.append(": ");
                    builder.append(thisChn.getString("chn_name"));
                    builder.append("\n");

                    builder.append("总票数: ");
                    builder.append(thisSum);
                    if (j > 0) {
                        builder.append(" (落后: ");
                        builder.append(prevSum - thisSum);
                        builder.append("票)");
                    }
                    builder.append("\n");

                    builder.append("票增数: ");
                    builder.append(thisNum);
                    if (j > 0) {
                        builder.append(" (追赶: ");
                        builder.append(+(thisNum - prevNum));
                        builder.append("票)");
                    }
                    builder.append("\n");

                    builder.append("得票率: ");
                    builder.append(String.format("%.2f%%", thisRatio / 100.0));
                    if (hasPrev) {
                        JSONArray prevResult = root.getJSONArray("prevResult");
                        for (int k = 0; k < prevResult.length(); k++) {
                            JSONObject prevChn = prevResult.getJSONObject(k);
                            if (prevChn.getInt("character_id") == thisChnId) {
                                int prevRatio = prevChn.getInt("ballot_ratio");
                                int diffRatio = thisRatio - prevRatio;
                                builder.append(" (");
                                builder.append(String.format("%+.2f%%", diffRatio / 100.0));
                                builder.append(")");
                                break;
                            }
                        }
                    }
                    builder.append("\n");

                    builder.append("真爱票: ");
                    builder.append(loveCount);
                    builder.append("\n");

                    builder.append("<br>\n");

                    prevSum = thisSum;
                    prevNum = thisNum;
                }
            });
        }

        return new HandlerResult(builder.toString(), getUnionData(idToChnName, unionVoteData));
    }

    public static class HandlerResult {
        private String voteData;
        private Map<Integer, String> unionData;

        private HandlerResult(String voteData, Map<Integer, String> unionData) {
            this.voteData = voteData;
            this.unionData = unionData;
        }

        public String getVoteData() {
            return voteData;
        }

        public Map<Integer, String> getUnionData() {
            return unionData;
        }
    }

    private static Map<Integer, String> getUnionData(Map<Integer, String> idToChnName, Map<Integer, Map<String, Integer>> unionVoteData) {
        Map<Integer, String> unionData = new HashMap<>();
        unionVoteData.forEach((voteCount, voteMap) -> {
            StringBuilder builder = new StringBuilder();
            builder.append("====");
            builder.append(voteCount);
            builder.append("连记分析====");
            builder.append("\n");

            voteMap.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue() - e1.getValue())
                    .limit(500)
                    .forEach(e -> {
                        String collect = Stream.of(e.getKey().split(","))
                                .map(idText -> idToChnName.get(Integer.valueOf(idText)))
                                .collect(Collectors.joining(" + "));
                        builder.append(collect);
                        builder.append(" = ");
                        builder.append(e.getValue());
                        builder.append("\n");
                    });

            builder.append("共有");
            builder.append(voteMap.size());
            builder.append("条，最多显示500条。");
            builder.append("\n");

            unionData.put(voteCount, builder.toString());
        });
        return unionData;
    }

    private static void appendHeader(StringBuilder builder, JSONObject current, int dataCount) {
        builder.append(current.getString("title"));
        appendQuote(builder, "本日总投票人数: " + dataCount);
        builder.append("\n<br/>\n");
    }

    private static void appendVoteData(StringBuilder builder, Map<Integer, Integer> personVoteCount,
                                       int groupCount, int dataCount) {
        Integer loveVoteCount = personVoteCount.get(0);
        builder.append("本日技术分析: \n");

        if (loveVoteCount != null) {
            builder.append("投真爱人数: ");
            builder.append(loveVoteCount);
            appendQuote(builder, percent(loveVoteCount, dataCount));
            builder.append("\n");
        }

        for (int i = 0; i < groupCount; i++) {
            Integer countVoteCount = personVoteCount.get(i + 1);
            if (countVoteCount != null) {
                builder.append("投").append(i + 1).append("票人数: ").append(countVoteCount);
                appendQuote(builder, percent(countVoteCount, dataCount));
                builder.append("\n");
            }
        }
        builder.append("<br/>\n");
    }

    private static <T> Integer safeGet(Map<T, Integer> map, T key) {
        return map.getOrDefault(key, 0);
    }

    private static <T> void increment(Map<T, Integer> map, T key) {
        map.put(key, safeGet(map, key) + 1);
    }

    private static String percent(double a, double b) {
        return String.format("%.2f%%", a / b * 100);
    }

    private static void appendQuote(StringBuilder builder, String string) {
        builder.append(" (");
        builder.append(string);
        builder.append(")");
    }


}
