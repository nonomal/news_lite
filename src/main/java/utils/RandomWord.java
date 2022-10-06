package utils;

import java.util.List;
import java.util.Random;

public class RandomWord {
    private final Random random;
    private final List<String> list = Common.words;

    public RandomWord() {
        this.random = new Random();
    }

    public void get() {
        Common.console(list.get(random.nextInt(list.size())));
    }
}