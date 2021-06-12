package com.news;

import java.io.IOException;
import java.net.URL;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class SyndParser {

    public SyndFeed parseFeed(String url) throws IllegalArgumentException, FeedException, IOException {
        return new SyndFeedInput().build(new XmlReader(new URL(url)));
    }


//    public void printRSSContent(SyndFeed feed) {
//
//        for (Object object : feed.getEntries()) {
//            SyndEntry entry = (SyndEntry) object;
//            System.out.println(entry.getTitle());
//            System.out.println(entry.getLink());
//
//            SyndContent content = entry.getDescription();
//            if (content != null)
//                System.out.println(content.getValue());
//            System.out.println(entry.getPublishedDate());
//            System.out.println();
//        }
//    }
}