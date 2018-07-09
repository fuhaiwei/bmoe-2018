package fuhaiwei.bmoe2018.autorun;

import fuhaiwei.bmoe2018.handler.Handler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.IntStream;

import static fuhaiwei.bmoe2018.spider.BmoeSpider.fetchCurrent;
import static fuhaiwei.bmoe2018.spider.BmoeSpider.fetchData;
import static fuhaiwei.bmoe2018.utils.FileUtil.writeText;

public class RunTask {

    public static final DateTimeFormatter DATE_FORMATTER;
    public static final DateTimeFormatter DATE_TIME_FORMATTER;

    private static final STGroupFile ST_GROUP_FILE;

    static {
        ST_GROUP_FILE = new STGroupFile("src/main/java/fuhaiwei/bmoe2018/template/template.stg", '$', '$');
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
        File[] files = new File("output/" + date)
                .listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".txt"));
        Objects.requireNonNull(files);
        
        Arrays.sort(files, Comparator.reverseOrder());

        ST bmoe2018 = ST_GROUP_FILE.getInstanceOf("bmoe2018");

        bmoe2018.add("fileNames", IntStream.range(0, files.length).boxed()
                .map(index -> buildFileName(files, index)).toArray());
        bmoe2018.add("fileContents", IntStream.range(0, files.length).boxed()
                .map(index -> buildFileContent(files, index)).toArray());

        return bmoe2018.render();
    }

    private static String buildFileName(File[] files, int index) {
        ST fileName = ST_GROUP_FILE.getInstanceOf("fileName");
        fileName.add("fileId", "txt" + index);
        fileName.add("fileName", files[index].getName());
        fileName.add("active", index == 2);
        return fileName.render();
    }

    private static String buildFileContent(File[] files, int index) {
        ST fileContent = ST_GROUP_FILE.getInstanceOf("fileContent");
        fileContent.add("fileId", "txt" + index);
        try {
            fileContent.add("fileContent", Files.readAllLines(files[index].toPath()));
        } catch (IOException e) {
            fileContent.add("fileContent", e.getMessage());
        }
        fileContent.add("active", index == 2);
        return fileContent.render();
    }

}