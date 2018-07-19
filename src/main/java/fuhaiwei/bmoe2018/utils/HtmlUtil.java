package fuhaiwei.bmoe2018.utils;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.IntStream;

public abstract class HtmlUtil {

    private static final STGroupFile ST_GROUP_FILE;

    static {
        ST_GROUP_FILE = new STGroupFile("src/main/java/fuhaiwei/bmoe2018/template/template.stg", '$', '$');
    }

    public static String buildHtml(String dateText, int groupCount) {
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

}
