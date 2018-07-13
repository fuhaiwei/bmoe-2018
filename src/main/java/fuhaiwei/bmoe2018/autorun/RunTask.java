package fuhaiwei.bmoe2018.autorun;

import fuhaiwei.bmoe2018.handler.Handler.HandlerResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.IntStream;

import static fuhaiwei.bmoe2018.handler.Handler.handleData;
import static fuhaiwei.bmoe2018.spider.BmoeSpider.fetchCurrent;
import static fuhaiwei.bmoe2018.spider.BmoeSpider.fetchData;
import static fuhaiwei.bmoe2018.utils.FileUtil.readText;
import static fuhaiwei.bmoe2018.utils.FileUtil.writeText;
import static java.time.format.DateTimeFormatter.ofPattern;

public class RunTask {

    private static final STGroupFile ST_GROUP_FILE;

    static {
        ST_GROUP_FILE = new STGroupFile("src/main/java/fuhaiwei/bmoe2018/template/template.stg", '$', '$');
    }

    public static void main(String[] args) {
//        Stream.of(
//                "2018-07-04",
//                "2018-07-05",
//                "2018-07-06",
//                "2018-07-07",
//                "2018-07-08",
//                "2018-07-09"
//        ).forEach(RunTask::rebuild);
        runFetchTask();
        System.out.println("Done!");
    }

    private static void runFetchTask() {
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

            writeText(buildHtml(dateText, groupCount), new File("output/bmoe2018.html"));
        }
    }

    private static void rebuild(String dateText) {
        try {
            System.out.println("Rebuild: " + dateText);
            String currentText = readText(String.format("data/%s/current.txt", dateText));
            JSONObject current = new JSONObject(currentText);
            JSONArray voteGroups = current.getJSONArray("voteGroups");
            if (voteGroups == null || voteGroups.length() == 0) {
                return;
            }
            int groupCount = voteGroups.length();

            File[] files = new File("data", dateText).listFiles(filterData());
            Objects.requireNonNull(files);
            Arrays.sort(files);

            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                System.out.println("Rebuild: " + dateText + "/" + file.getName());
                JSONArray data = new JSONArray(readText(file.getPath()));

                HandlerResult result = handleData(current, data, i == files.length - 1);
                String newFileName = file.getName().replace(':', '_');
                writeText(result.getVoteData(), fileFormat("output/%s/%s", dateText, newFileName));

                result.getUnionData().forEach((voteCount, unionData) -> {
                    writeText(unionData, fileFormat("output/%s/连记%d.txt", dateText, voteCount));
                });

                writeText(buildHtml(dateText, groupCount), fileFormat("output/%s/bmoe2018.html", dateText, dateText));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static FileFilter filterData() {
        return pathname -> pathname.getName().matches("\\d{2}:\\d{2}\\.txt");
    }

    private static String buildHtml(String dateText, int groupCount) {
        File[] files = new File("output/" + dateText)
                .listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".txt"));
        Objects.requireNonNull(files);

        Arrays.sort(files, Comparator.reverseOrder());

        ST bmoe2018 = ST_GROUP_FILE.getInstanceOf("bmoe2018");

        bmoe2018.add("fileNames", IntStream.range(0, files.length).boxed()
                .map(index -> buildFileName(files, index, groupCount)).toArray());
        bmoe2018.add("fileContents", IntStream.range(0, files.length).boxed()
                .map(index -> buildFileContent(files, index, groupCount)).toArray());

        return bmoe2018.render();
    }

    private static String buildFileName(File[] files, int index, int groupCount) {
        ST fileName = ST_GROUP_FILE.getInstanceOf("fileName");
        fileName.add("fileId", "txt" + index);
        fileName.add("fileName", files[index].getName());
        fileName.add("active", index == groupCount - 1);
        return fileName.render();
    }

    private static String buildFileContent(File[] files, int index, int groupCount) {
        ST fileContent = ST_GROUP_FILE.getInstanceOf("fileContent");
        fileContent.add("fileId", "txt" + index);
        try {
            fileContent.add("fileContent", Files.readAllLines(files[index].toPath()));
        } catch (IOException e) {
            fileContent.add("fileContent", e.getMessage());
        }
        fileContent.add("active", index == groupCount - 1);
        return fileContent.render();
    }

    private static String fileName() {
        return LocalDateTime.now().format(ofPattern("yyyy-MM-dd/HH:mm"));
    }

    private static File fileFormat(String format, Object... args) {
        return new File(String.format(format, args));
    }

}