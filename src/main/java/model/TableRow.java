package model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Getter
@Setter
@AllArgsConstructor
public class TableRow implements Comparable<TableRow> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MMM HH:mm", Locale.ENGLISH);
    private String source;
    private String title;
    private String describe;
    private String date;
    private String link;

    @Override
    public String toString() {
        return this.getTitle() + "\n" + this.getLink() + "\n" + this.getDescribe() + "\n" +
                this.getSource() + " - " + this.getDate();
    }

    @Override
    public int compareTo(TableRow o) {
        try {
            Date date1 = DATE_FORMAT.parse(o.getDate());
            Date date2 = DATE_FORMAT.parse(this.getDate());
            return date2.compareTo(date1);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}

