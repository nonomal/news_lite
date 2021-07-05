package com.news;

import java.util.TimerTask;

class MyTimerTask extends TimerTask {
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