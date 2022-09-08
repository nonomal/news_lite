package model;

import lombok.Data;

@Data
public class RssSource {
    private Integer id;
    private String source;
    private String link;
    private Boolean isActive;
}
