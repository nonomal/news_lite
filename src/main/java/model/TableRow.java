package model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TableRow {
    private String source;
    private String title;
    private String describe;
    private String date;
    private String link;

    @Override
    public String toString() {
        return this.getTitle() + "\n" + this.getLink() + "\n" +  this.getDescribe() + "\n" +
                this.getSource() + " - " + this.getDate();
    }
}

