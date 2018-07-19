package fuhaiwei.bmoe2018.autorun;

import fuhaiwei.bmoe2018.handler.Handler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

import static fuhaiwei.bmoe2018.handler.Handler.handleData;
import static fuhaiwei.bmoe2018.utils.FileUtil.readText;
import static fuhaiwei.bmoe2018.utils.FileUtil.writeText;
import static fuhaiwei.bmoe2018.utils.HtmlUtil.buildHtml;
import static java.time.format.DateTimeFormatter.ofPattern;

public class Rebuild {

    public static void main(String[] args) {
        if (args.length == 1) {
            String dateText = args[0];
            rebuild(dateText);
        } else {
            rebuild(LocalDate.now().format(ofPattern("yyyy-MM-dd")));
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

                Handler.HandlerResult result = handleData(current, data, i == files.length - 1);
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

    private static File fileFormat(String format, Object... args) {
        return new File(String.format(format, args));
    }

}
