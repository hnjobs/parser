package com.emilburzo.hnjobs.parser;

import com.emilburzo.hnjobs.http.HttpClient;
import com.emilburzo.hnjobs.main.Main;
import com.emilburzo.hnjobs.pojo.Job;
import com.emilburzo.hnjobs.pojo.JobThread;
import com.emilburzo.hnjobs.util.DateUtil;
import com.google.gson.Gson;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Logger;

import static com.emilburzo.hnjobs.util.Constants.*;

public class Parser {

    private final static Logger logger = Logger.getLogger("Parser");

    private JobThread thread;

    private int jobs = 0;
    private int jobsDeleted = 0;

    public Parser() throws IOException, ParseException {
        loadJobThread();

        parseJobThread();

        cleanOldJobs();
    }

    private void loadJobThread() throws IOException, ParseException {
        // fetch submissions from the "whoishiring" user
        String content = HttpClient.getUrl(URL.WHOISHIRING_URL);

        // pass the content to Jsoup for parsing
        Document doc = Jsoup.parse(content);

        // submissions rows have the class name "storylink"
        Elements storylinks = doc.getElementsByClass(Parse.STORYLINK);

        for (Element storyLink : storylinks) {
            String linkId = storyLink.attr(Parse.ID);
            String linkText = storyLink.text();

            // look only for the last "Who is hiring?" thread
            if (linkText.contains(Parse.WHO_IS_HIRING)) {
                logger.info(String.format("Found relevant thread: %s (%s)", linkText, linkId));

                thread = new JobThread(linkId, linkText);

                return;
            }
        }
    }

    private void parseJobThread() throws IOException {
        if (thread == null) {
            logger.warning("No job thread found");
            return;
        }

        parseJobThread(URL.BASE_URL + "item?id=" + thread.linkId + "&p=1");

        logger.info(String.format("Found %d jobs", jobs));
    }

    private void parseJobThread(String url) throws IOException {
        logger.info(String.format("Parsing job thread %s - %s", thread.text, url));

        // get all the html content from the thread
        String content = HttpClient.getUrl(url);

        // parse it
        Document doc = Jsoup.parse(content);

        // we are interested in the comment tree
        Elements commentTrees = doc.getElementsByClass(Parse.COMMENT_TREE);

        // there is only one comment tree
        // but since are extracting by the class name
        // we get back a list
        for (Element commentTree : commentTrees) {
            // each comment is contained in a "aThing"
            Elements aThings = commentTree.getElementsByClass(Parse.COMMENT_TREE_ATHING);

            for (Element aThing : aThings) {
                // figure out the indentation level of the comment
                Element ind = aThing.getElementsByClass(Parse.INDENT).first();
                Element indImg = ind.child(0);

                // if it's not the parent comment, move on
                if (indImg.getElementsByAttributeValue("width", "0").isEmpty()) {
                    continue;
                }

                parseJob(aThing);
            }
        }

        // do we have a `More` link?
        Elements moreLinks = doc.getElementsByClass("morelink");

        // if we do, follow it
        for (Element moreLink : moreLinks) {
            String href = moreLink.attr("href");

            parseJobThread(String.format("%s%s", URL.BASE_URL, href));
        }
    }

    private void parseJob(Element aThing) {
        Element a = aThing.getElementsByClass(Parse.COMMENT_HEAD).first();
        Element b = a.getElementsByTag("a").first();

        if (b == null) {
            return;
        }

        String author = b.text();

        Element age = aThing.getElementsByClass(Parse.AGE).first().child(0);

        String link = age.attr(Parse.HREF);
        String linkAge = age.text();

        Element body = aThing.getElementsByClass(Parse.COMMENT).first();

        // remove "reply" link
        body.getElementsByClass(Parse.REPLY).remove();

        // parse post id
        String postId = link.substring(link.indexOf("=") + 1);

        // save everything neatly in a POJO so we can pass it to Gson when saving
        Job job = new Job();
        job.author = author;
        job.timestamp = DateUtil.getAbsoluteDate(linkAge).getTime();
        job.src = thread.id;
        job.body = Jsoup.parse(body.toString()).text();
        job.bodyHtml = body.toString();

        // persist to elasticsearch
        saveJob(postId, job);

        jobs++;
    }

    private void saveJob(String id, Job job) {
        IndexResponse response = Main.getClient().prepareIndex(Index.HNJOBS, Type.JOB, id)
                .setSource(new Gson().toJson(job))
                .get();
    }

    /**
     * remove job posts that are too old
     */
    private void cleanOldJobs() {
        logger.info("Cleaning old jobs");

        ConstantScoreQueryBuilder qb = QueryBuilders.constantScoreQuery(QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery(Field.SRC, thread.id)));

        SearchResponse resp = Main.getClient().prepareSearch(Index.HNJOBS)
                .setTypes(Type.JOB)
                .setScroll(new TimeValue(60000))
                .setQuery(qb)
                .setSize(100).execute().actionGet();

        while (true) {
            for (SearchHit hit : resp.getHits().getHits()) {
                Main.getClient().prepareDelete(Index.HNJOBS, Type.JOB, hit.getId()).get();

                jobsDeleted++;
            }

            resp = Main.getClient().prepareSearchScroll(resp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();

            // stop when there are no more hits
            if (resp.getHits().getHits().length == 0) {
                break;
            }
        }

        logger.info(String.format("Deleted %d old jobs", jobsDeleted));
    }
}
