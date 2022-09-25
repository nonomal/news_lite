package model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TableRow {
    private String source;
    private String title;
    private String describe;
    private String date;
    private String link;
}

