package fuhaiwei.bmoe2018.autorun;

import fuhaiwei.bmoe2018.handler.Handler.HandlerResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static fuhaiwei.bmoe2018.handler.Handler.handleData;
import static fuhaiwei.bmoe2018.spider.BmoeSpider.fetchCurrent;
import static fuhaiwei.bmoe2018.spider.BmoeSpider.fetchData;
import static fuhaiwei.bmoe2018.utils.FileUtil.writeText;
import static fuhaiwei.bmoe2018.utils.HtmlUtil.buildHtml;
import static java.time.format.DateTimeFormatter.ofPattern;

public class RunTask {

    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            try {
                executeTask();
                break;
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Done!");
    }

    private static void executeTask() {
        JSONObject current = fetchCurrent();
        if (current != null) {
            JSONArray voteGroups = current.getJSONArray("voteGroups");
            if (voteGroups == null || voteGroups.length() == 0) {
                return;
            }
            int groupCount = voteGroups.length();

            String dateText = LocalDate.now().format(ofPattern("yyyy-MM-dd"));
            writeText(current.toString(), fileFormat("data/%s/current.txt", dateText));

            JSONArray data = fetchData();
            writeText(data.toString(), fileFormat("data/%s.txt", fileName()));

            HandlerResult result = handleData(current, data, true);
            writeText(result.getVoteData(), fileFormat("output/%s.txt", fileName()));

            result.getUnionData().forEach((voteCount, unionData) -> {
                writeText(unionData, fileFormat("output/%s/连记%d.txt", dateText, voteCount));
            });

            String htmlText = buildHtml(dateText, groupCount);
            writeText(htmlText, new File("output/bmoe2018.html"));
            writeText(htmlText, fileFormat("output/%s/bmoe2018.html", dateText));
        }
    }

    private static String fileName() {
        return LocalDateTime.now().format(ofPattern("yyyy-MM-dd/HH:mm"));
    }

    private static File fileFormat(String format, Object... args) {
        return new File(String.format(format, args));
    }

}