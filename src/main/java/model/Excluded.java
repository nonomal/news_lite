package model;

import lombok.Data;

@Data
public class Excluded {
    private String word;

    public Excluded(String word) {
        this.word = word;
    }
}