package fuhaiwei.bmoe2018.autorun

import fuhaiwei.bmoe2018.utils.FileUtil.readText
import fuhaiwei.bmoe2018.utils.FileUtil.writeText
import fuhaiwei.bmoe2018.utils.HtmlUtil.buildHtml
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter.ofPattern
import java.util.function.Consumer

fun main(args: Array<String>) {
    val dateText = LocalDate.now().format((ofPattern("yyyy-MM-dd")))
    val fileName = "output/$dateText/${LocalTime.now().format(ofPattern("HH_mm"))}.txt"
    writeText(getVoteInfo(), File(fileName))
    writeText(buildHtml(dateText, 1), File("output/bmoe2018.html"))
    writeText(buildHtml(dateText, 1), File("output/$dateText/bmoe2018.html"))
    println("done!")
}

private fun getVoteInfo(): String {
    val builder = StringBuilder()

    fetchResult("https://api.bilibili.com/pgc/moe/2018/2/api/schedule/current", Consumer {
        val current = it.getJSONObject("result")
        builder.append("${LocalDate.now()} ${current["title"]}")
        builder.append('\n')
        builder.append("<br>")
        builder.append('\n')

        val voteGroups = current.getJSONArray("voteGroups")
        for (i in 0 until voteGroups.length()) {
            val voteGroup = voteGroups.getJSONObject(i)
            builder.append("<font color='red'>=== ${voteGroup["group_name"]} ===</font>")
            builder.append('\n')
            builder.append("<br>")
            builder.append('\n')

            fetchGroup(voteGroup.getInt("group_id"), Consumer {
                val voteInfo = it.getJSONArray("result")
                val hasPrev = it.has("prevResult")
                val length = Math.min(10, voteInfo.length())

                var prevSum = 0
                var prevNum = 0

                for (j in 0 until length) {
                    val thisChn = voteInfo.getJSONObject(j)

                    val thisSum = thisChn.getInt("ballot_sum")
                    val thisNum = thisChn.getInt("ballot_num")
                    val thisRatio = thisChn.getInt("ballot_ratio")

                    builder.append("${j + 1}: ${thisChn["chn_name"]}")
                    builder.append('\n')
                    builder.append("总票数: $thisSum")
                    if (j > 0) {
                        builder.append(" (落后: ${prevSum - thisSum}票)")
                    }
                    builder.append('\n')
                    builder.append("票增数: $thisNum")
                    if (j > 0) {
                        builder.append(" (追赶: ${thisNum - prevNum}票)")
                    }
                    builder.append('\n')
                    builder.append("得票率: ${String.format("%.2f%%", thisRatio / 100.0)}")
                    if (hasPrev) {
                        val prevResult = it.getJSONArray("prevResult")
                        val prevChn = prevResult.getJSONObject(j)
                        val prevRatio = prevChn.getInt("ballot_ratio")
                        val diffRatio = thisRatio - prevRatio
                        builder.append(" (${String.format("%+.2f%%", diffRatio / 100.0)})")
                    }
                    builder.append('\n')
                    builder.append("<br>")
                    builder.append('\n')

                    prevSum = thisSum
                    prevNum = thisNum
                }
            })
        }
    })
    return builder.toString()
}

fun fetchResult(url: String, consumer: Consumer<JSONObject>) {
    val body = Jsoup.connect(url)
            .ignoreContentType(true)
            .execute()
            .body()
    val root = JSONObject(body)
    if (root.getInt("code") == 0 && root.getString("message") == "success") {
        consumer.accept(root)
    }
}

fun fetchGroup(groupId: Int, consumer: Consumer<JSONObject>) {
    fetchResult("https://api.bilibili.com/pgc/moe/2018/2/api/schedule/ranking?group_id=$groupId", Consumer {
        val thisText = it.toString()
        try {
            val readText = readText("output/data/$groupId")
            if (thisText != readText) {
                it.put("prevResult", JSONObject(readText).getJSONArray("result"))
                println("prev result set")
            }
        } catch (e: Exception) {
        }
        writeText(thisText, File("output/data/$groupId"))
        consumer.accept(it)
    })
}
