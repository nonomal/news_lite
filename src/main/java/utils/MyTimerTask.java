package utils;

import com.news.Gui;
import com.news.Search;

import java.util.TimerTask;

public class MyTimerTask extends TimerTask {
    @Override
    public void run() {
        Search search = new Search();
        if (Gui.autoUpdateNewsTop.getState()) {
            search.mainSearch("word");
        } else if (Gui.autoUpdateNewsBottom.getState()) {
            search.mainSearch("words");
        }
    }
}