package utils;

import database.JdbcQueries;

import java.util.Random;

public class RandomWord {
    final JdbcQueries jdbcQueries = new JdbcQueries();
    private final Random random = new Random();

    public void get() {
        Common.console("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
        Common.console(jdbcQueries.getRandomWord(random.nextInt(5002)));
    }
}