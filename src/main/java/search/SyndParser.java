package search;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import gui.Gui;
import utils.Common;

public class SyndParser {
    Long timeStart;
    Long timeEnd;
    Long searchTime;
    int longSearch = 2;

    public SyndFeed parseFeed(String url) throws IllegalArgumentException, FeedException, IOException {
        timeStart = System.currentTimeMillis();
        URLConnection urlConnection = new URL(url).openConnection();
        urlConnection.setConnectTimeout(1000);
        XmlReader reader = new XmlReader(urlConnection);

        // подсчёт времени поиска
        timeEnd = System.currentTimeMillis();
        searchTime = (timeEnd - timeStart) / 1000;
        DecimalFormat f = new DecimalFormat("##");

        String urlToConsole = url.replaceAll(("https://|http://|www."), "");
        urlToConsole = urlToConsole.substring(0, urlToConsole.indexOf("/"));

        if (!Gui.guiInTray.get() && searchTime > longSearch) Common.console("info: long search - " + urlToConsole
                + " - " + f.format(searchTime) + " s.");

        return new SyndFeedInput().build(reader);
    }
}