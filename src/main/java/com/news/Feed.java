package com.news;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Feed {
    final List<FeedMessage> entries = new ArrayList<>();

    public Feed(String title, String link, String pubDate) {
    }

    public List<FeedMessage> getMessages() {
        return entries;
    }
}

class FeedMessage {
    String title;
    String link;
    String pubDate;

    public boolean isSelTitle = Gui.isSelTitle;
    public boolean isSelLink = Gui.isSelLink;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setPubDate(String pubDate) {
        try {
            SimpleDateFormat oldDateFormat;
            if (Common.smi_number == 25) {
                oldDateFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm z", Locale.ENGLISH); // Nasa
            } else if (Common.smi_number == 1 || (Common.smi_number == 26)) {
                oldDateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH); // Яндекс, Яндекс.Космос
            } else {
                oldDateFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
            }
            SimpleDateFormat newDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            Date date = oldDateFormat.parse(pubDate);
            this.pubDate = newDateFormat.format(date);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            //this.pubDate = Search.today;
        }
    }

    public String getLink() {
        return link;
    }

    public String getTitle() {
        return title;
    }

    public String getPubDate() {
        return pubDate;
    }

    @Override
    public String toString() {
        if (isSelTitle && isSelLink) return title + ":\n" + link + "\nДата публикации: " + pubDate;
        else if (isSelTitle) return title;
        else if (isSelLink) return link;
        else return title;
    }
}