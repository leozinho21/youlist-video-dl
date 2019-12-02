package com.thesis.home.youlist.services;

import com.thesis.home.youlist.exceptions.ExtractionException;
import com.thesis.home.youlist.exceptions.ParsingException;
import com.thesis.home.youlist.helpers.Downloader;
import com.thesis.home.youlist.helpers.StringUtils;
import com.thesis.home.youlist.model.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.net.URLEncoder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class YoutubeSearchEngine{

    private VideoPreviewInfoSearchCollector collector;

    public YoutubeSearchEngine() {
        collector = new VideoPreviewInfoSearchCollector();
    }

    public VideoPreviewInfoSearchCollector search(String query, int page, String languageCode, Downloader downloader)
            throws IOException, ExtractionException {

        String url = "https://www.youtube.com/results"
                + "?search_query=" + URLEncoder.encode(query, "UTF-8")
                + "&page=" + Integer.toString(page)
                + "&filters=" + "video";

        String site;
        if(StringUtils.notEmpty(languageCode)) {
            site  = downloader.download(url, languageCode);
        }
        else {
            site = downloader.download(url);
        }


        Document doc = Jsoup.parse(site, url);
        Element list = doc.select("ol[class=\"item-section\"]").first();

        for (Element item : list.children()) {

            Element el;

            // both types of spell correction item
            if (!((el = item.select("div[class*=\"spell-correction\"]").first()) == null)) {
                collector.setSuggestion(el.select("a").first().text());
                if(list.children().size() == 1) {
                    Log.d("Did you mean: " + el.select("a").first().text());
                }
                // search message item
            } else if (!((el = item.select("div[class*=\"search-message\"]").first()) == null)) {
                Log.d(el.text());

                // video item type
            } else if (!((el = item.select("div[class*=\"yt-lockup-video\"").first()) == null)) {
                collector.commit(extractPreviewInfo(el));
            } else {
                collector.addError(new Exception("unexpected element found:\"" + el + "\""));
            }
        }

        return collector;
    }

    public ArrayList<String> suggestionList(String query, String contentCountry, Downloader dl)
            throws IOException, ParsingException {

        ArrayList<String> suggestions = new ArrayList<>();

        String url = "https://suggestqueries.google.com/complete/search"
                + "?client=" + ""
                + "&output=" + "toolbar"
                + "&ds=" + "yt"
                + "&hl=" + URLEncoder.encode(contentCountry, "UTF-8")
                + "&q=" + URLEncoder.encode(query, "UTF-8");


        String response = dl.download(url);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        org.w3c.dom.Document doc ;

        try {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(new InputSource(
                    new ByteArrayInputStream(response.getBytes("utf-8"))));
            doc.getDocumentElement().normalize();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ParsingException("Could not parse document.");
        }

        try {
            NodeList nList = doc.getElementsByTagName("CompleteSuggestion");
            for (int temp = 0; temp < nList.getLength(); temp++) {

                NodeList nList1 = doc.getElementsByTagName("suggestion");
                Node nNode1 = nList1.item(temp);
                if (nNode1.getNodeType() == Node.ELEMENT_NODE) {
                    org.w3c.dom.Element eElement = (org.w3c.dom.Element) nNode1;
                    suggestions.add(eElement.getAttribute("data"));
                }
            }
            return suggestions;
        } catch(Exception e) {
            throw new ParsingException("Could not get suggestions form document.", e);
        }
    }

    private VideoInfoCreator extractPreviewInfo(final Element item) {
        return new VideoInfoCreator(item);
    }
}
