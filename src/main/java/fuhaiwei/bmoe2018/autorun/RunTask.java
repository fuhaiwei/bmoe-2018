package fuhaiwei.bmoe2018.autorun;

import fuhaiwei.bmoe2018.handler.Handler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static fuhaiwei.bmoe2018.spider.BmoeSpider.fetchCurrent;
import static fuhaiwei.bmoe2018.spider.BmoeSpider.fetchData;
import static fuhaiwei.bmoe2018.utils.FileUtil.writeText;

public class RunTask {

    public static final DateTimeFormatter DATE_FORMATTER;
    public static final DateTimeFormatter DATE_TIME_FORMATTER;

    static {
        DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd/HH:mm");
    }

    public static void main(String[] args) {
        JSONObject current = fetchCurrent();
        if (current != null) {
            String dateText = DATE_FORMATTER.format(LocalDateTime.now());
            writeText(current.toString(), new File(String.format("data/%s/current.txt", dateText)));

            JSONArray data = fetchData();
            String dataText = data.toString();

            String datetimeText = DATE_TIME_FORMATTER.format(LocalDateTime.now());
            writeText(dataText, new File(String.format("data/%s.txt", datetimeText)));

            Handler.handleData(current, data);
            writeText(buildHtml(dateText), new File("output/bmoe2018.html"));
        }
        System.out.println("Done!");
    }

    private static String buildHtml(String date) {
        StringBuilder builder = new StringBuilder();
        try {
            List<String> strings = Files.readAllLines(new File("src/main/java/fuhaiwei/bmoe2018/template/template.html").toPath());
            for (int i = 0; i < 14; i++) {
                builder.append(strings.get(i)).append("\n");
            }

            File[] files = new File("output/" + date)
                    .listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".txt"));
            if (files != null) {
                Arrays.sort(files, Comparator.reverseOrder());
            }

            if (files == null || files.length == 0) {
                builder.append("本日暂无数据");
            } else {
                builder.append("<ul id=\"myTab\" class=\"nav nav-tabs\">\n");
                for (int i = 0; i < files.length; i++) {
                    appendTab(builder, files, i);
                }
                builder.append("</ul>\n");


                builder.append("<div id=\"myTabContent\" class=\"tab-content\" style=\"padding: 8px\">\n");
                for (int i = 0; i < files.length; i++) {
                    appendContent(builder, files, i);
                }
                builder.append("</div>\n");
            }

            for (int i = 14; i < strings.size(); i++) {
                builder.append(strings.get(i)).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private static void appendTab(StringBuilder builder, File[] files, int index) {
        if (index == 2) {
            builder.append("   <li class=\"active\">\n");
        } else {
            builder.append("   <li>\n");
        }
        builder.append("      <a href=\"#txt").append(index).append("\" data-toggle=\"tab\">\n");
        builder.append("         ").append(files[index].getName()).append("\n");
        builder.append("      </a>\n");
        builder.append("   </li>\n");
    }

    private static void appendContent(StringBuilder builder, File[] files, int index) throws IOException {
        if (index == 2) {
            builder.append("   <div class=\"tab-pane fade in active\" id=\"txt2\">\n");
        } else {
            builder.append("   <div class=\"tab-pane fade\" id=\"txt").append(index).append("\">\n");
        }
        List<String> strings = Files.readAllLines(files[index].toPath());
        strings.forEach(s -> builder.append("      <p>").append(s).append("</p>\n"));
        builder.append("   </div>\n");
    }

}