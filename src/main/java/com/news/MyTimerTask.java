package com.news;

import java.util.TimerTask;

class MyTimerTask extends TimerTask {
    @Override
    public void run() {
        if (Gui.autoUpdateNewsTop.getState()) {
            Search.mainSearch("word");
        } else if (Gui.autoUpdateNewsBottom.getState()) {
            //Search.keywordsSearch();
            Search.mainSearch("words");
        }
    }
}