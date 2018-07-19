package fuhaiwei.bmoe2018.autorun

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

            fetchResult("https://api.bilibili.com/pgc/moe/2018/2/api/schedule/ranking?group_id=${voteGroup["group_id"]}", Consumer {
                val voteInfo = it.getJSONArray("result")
                val length = Math.min(10, voteInfo.length())

                var prevVote = 0
                for (j in 0 until length) {
                    val chn = voteInfo.getJSONObject(j)
                    val thisVote = chn.getInt("ballot_sum")

                    builder.append("${j + 1}: ${chn["chn_name"]}")
                    builder.append('\n')
                    builder.append("总票数: $thisVote")
                    if (j > 0) {
                        builder.append(" (落后: ${prevVote - thisVote}票)")
                    }
                    builder.append('\n')
                    builder.append("票增数: ${chn["ballot_num"]}")
                    builder.append('\n')
                    builder.append("得票率: ${String.format("%.2f%%", chn.getInt("ballot_ratio") / 100.0)}")
                    builder.append('\n')
                    builder.append("<br>")
                    builder.append('\n')

                    prevVote = thisVote
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
