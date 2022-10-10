package utils;

import database.JdbcQueries;
import database.SQLite;
import model.Dates;

import java.time.LocalDate;
import java.util.List;

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

        new SQLite().openConnection();
        List<Dates> dates = new JdbcQueries().getDates();

        for (Dates date : dates) {
            if (date.getMonth() == month && date.getDay() == day) {
                Common.console("Сегодня: " + date.getType() + " - " + date.getDescription());
            }
            if (date.getMonth() == monthTomorrow && date.getDay() == dayTomorrow) {
                Common.console("Завтра: " + date.getType() + " - " + date.getDescription());
            }
        }


    }
}
