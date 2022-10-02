package model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Favorite {
    private String title;
    private String link;
    private String date;
}