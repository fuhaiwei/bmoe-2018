package fuhaiwei.bmoe2018.spider;

import fuhaiwei.bmoe2018.utils.FileUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static fuhaiwei.bmoe2018.utils.FileUtil.readText;
import static fuhaiwei.bmoe2018.utils.FileUtil.writeText;
import static java.time.format.DateTimeFormatter.ofPattern;

public abstract class BmoeSpider {

    public static JSONObject fetchCurrent() {
        String url = "https://api.bilibili.com/pgc/moe/2018/2/api/schedule/current";
        int err = 0;
        while (true) {
            try {
                String jsonText = Jsoup.connect(url)
                        .ignoreContentType(true)
                        .timeout(3000)
                        .execute()
                        .body();
                JSONObject root = new JSONObject(jsonText);
                if (isSuccess(root)) {
                    JSONObject result = root.getJSONObject("result");
                    JSONObject object = new JSONObject();
                    object.put("title", result.getString("title"));
                    object.put("voteGroups", result.getJSONArray("voteGroups"));
                    return object;
                } else {
                    break;
                }
            } catch (IOException | RuntimeException e) {
                if (++err > 10) {
                    System.out.println("失败次数大于10次，链接为：" + url);
                    e.printStackTrace();
                    break;
                }
            }
        }
        return null;
    }

    public static JSONArray fetchData() {
        int threadCount = 5;
        AtomicInteger count = new AtomicInteger(threadCount);
        JSONArray array = new JSONArray();
        for (int i = 0; i < threadCount; i++) {
            int finalI = i + 1;
            new Thread(() -> {
                JSONArray data = fetchData(finalI, threadCount);
                synchronized (array) {
                    data.forEach(array::put);
                    if (count.decrementAndGet() == 0) {
                        array.notify();
                    }
                }
            }).start();
        }
        synchronized (array) {
            try {
                array.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    private static JSONArray fetchData(int start, int step) {
        String url = "https://api.bilibili.com/pgc/moe/2018/2/web/realtime/votes?pn=%d";
        JSONArray array = new JSONArray();
        int page = start;
        int err = 0;
        while (true) {
            try {
                String dateText = LocalDate.now().format(ofPattern("yyyy-MM-dd"));
                String pathname = String.format("data/%s/page/%05d.txt", dateText, page);
                File file = new File(pathname);
                String jsonText;
                if (!file.exists() || !isFullData(jsonText = FileUtil.readText(pathname))) {
                    jsonText = Jsoup.connect(String.format(url, page))
                            .ignoreContentType(true)
                            .timeout(3000)
                            .execute()
                            .body();
                    FileUtil.writeText(jsonText, file);
                    System.out.printf("已成功获取数据[page=%d,errpr=%d]%n", page, err);
                } else {
                    System.out.printf("已读取现有数据[page=%d]%n", page);
                }
                JSONObject root = new JSONObject(jsonText);
                if (isSuccess(root)) {
                    JSONArray result = root.getJSONArray("result");
                    int length = result.length();

                    if (length == 0) {
                        break;
                    }
                    for (int i = 0; i < length; i++) {
                        array.put(result.getJSONObject(i));
                    }
                } else {
                    break;
                }
                page += step;
                err = 0;
            } catch (IOException | RuntimeException e) {
                if (++err > 10) {
                    System.out.println("失败次数大于10次，链接为：" + String.format(url, page));
                    e.printStackTrace();
                    break;
                }
            }
        }
        return array;
    }

    private static boolean isFullData(String jsonText) {
        JSONObject root = new JSONObject(jsonText);
        return isSuccess(root) && root.getJSONArray("result").length() == 50;
    }

    private static boolean isSuccess(JSONObject root) {
        return root.getInt("code") == 0 && "success".equals(root.getString("message"));
    }

    public static void fetchGroup(int groupId, Consumer<JSONObject> consumer) {
        String urlPrefix = "https://api.bilibili.com/pgc/moe/2018/2/api/schedule/ranking?group_id=";
        fetchResult(urlPrefix + groupId, body -> {
            String readText = null;
            try {
                readText = readText("output/data/" + groupId);
            } catch (IOException ignored) {
            }
            JSONObject root = new JSONObject(body);
            if (readText != null && !body.equals(readText)) {
                root.put("prevResult", new JSONObject(readText).getJSONArray("result"));
            }
            writeText(body, new File("output/data/" + groupId));
            consumer.accept(root);
        });
    }

    private static void fetchResult(String url, Consumer<String> consumer) {
        int err = 0;
        while (true) {
            try {
                String body = Jsoup.connect(url)
                        .ignoreContentType(true)
                        .timeout(3000)
                        .execute()
                        .body();
                consumer.accept(body);
                break;
            } catch (IOException | RuntimeException e) {
                if (++err > 10) {
                    System.out.println("失败次数大于10次，链接为：" + url);
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}