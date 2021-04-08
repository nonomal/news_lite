import java.util.TimerTask;

class MyTimerTask extends TimerTask {
    @Override
    public void run() {
        if (Gui.autoUpdateNewsTop.getState()) {
            Search.mainSearch();
        } else if (Gui.autoUpdateNewsBottom.getState()) {
            Search.keywordsSearch();
        }
    }
}