package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomFact {
    private final Random random = new Random();
    private final List<String> facts = new ArrayList<>();

    public void get() {
        facts.add("1");
        facts.add("2");
        facts.add("3");
        facts.add("4");
        facts.add("5");
        facts.add("6");
        Common.console(facts.get(random.nextInt(facts.size())));
    }
}