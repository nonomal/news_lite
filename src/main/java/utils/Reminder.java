package utils;

import database.JdbcQueries;
import model.Dates;

import java.time.LocalDate;

public class Reminder {

    public void remind() {
        String [] now = LocalDate.now().toString().split("-");
        int year = Integer.parseInt(now[0]);
        int month = Integer.parseInt(now[1]);
        int day = Integer.parseInt(now[2]);

        String [] tomorrow = LocalDate.now().plusDays(1).toString().split("-");
        int yearTomorrow = Integer.parseInt(tomorrow[0]);
        int monthTomorrow = Integer.parseInt(tomorrow[1]);
        int dayTomorrow = Integer.parseInt(tomorrow[2]);

        for (Dates date : new JdbcQueries().getDates()) {
            if (date.getMonth() == month && date.getDay() == day) {
                if (date.getType().equals("День Рождения") && date.getYear() != 0) {
                    Common.console("Сегодня: " + date.getDescription() + " исполняется " +
                            (year - date.getYear()));
                } else {
                    Common.console("Сегодня: " + date.getType() + " - " + date.getDescription());
                }
            }

            if (date.getMonth() == monthTomorrow && date.getDay() == dayTomorrow) {
                if (date.getType().equals("День Рождения") && date.getYear() != 0) {
                    Common.console("Завтра: " + date.getDescription() + " исполняется " +
                            (yearTomorrow - date.getYear()));
                } else {
                    Common.console("Завтра: " + date.getType() + " - " + date.getDescription());
                }
            }
        }
    }

}
