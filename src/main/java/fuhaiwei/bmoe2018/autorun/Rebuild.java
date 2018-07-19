package fuhaiwei.bmoe2018.autorun;

import java.time.LocalDate;

import static java.time.format.DateTimeFormatter.ofPattern;

public class Rebuild {
    public static void main(String[] args) {
        if (args.length == 1) {
            String dateText = args[0];
            RunTask.rebuild(dateText);
        } else {
            RunTask.rebuild(LocalDate.now().format(ofPattern("yyyy-MM-dd")));
        }
    }
}
