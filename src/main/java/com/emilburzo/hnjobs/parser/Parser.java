package com.emilburzo.hnjobs.parser;

import com.emilburzo.hnjobs.http.HttpClient;
import com.emilburzo.hnjobs.main.Main;
import com.emilburzo.hnjobs.pojo.Job;
import com.emilburzo.hnjobs.pojo.JobThread;
import com.emilburzo.hnjobs.util.DateUtil;
import com.google.gson.Gson;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static com.emilburzo.hnjobs.util.Constants.*;

public class Parser {

    private final static Logger logger = Logger.getLogger("Parser");

    private static final int MONTHS_OLD_THRESHOLD = 2; // how many months ago is "old"

    private List<JobThread> threads = new ArrayList<>();

    private SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy");

    public Parser() throws IOException, ParseException {
        loadJobThreads();

        parseJobThreads();

        cleanOldJobs();
    }

    private void loadJobThreads() throws IOException, ParseException {
        // fetch submissions from the "whoishiring" user
        String content = HttpClient.getUrl(URL.WHOISHIRING_URL);

        // pass the content to Jsoup for parsing
        Document doc = Jsoup.parse(content);

        // submissions rows have the class name "storylink"
        Elements storylinks = doc.getElementsByClass(Parse.STORYLINK);

        for (Element storyLink : storylinks) {
            String linkId = storyLink.attr(Parse.HREF);
            String linkText = storyLink.text();

            // look only for "Who is hiring?" threads
            // from the past two months
            // i.e.: the last two submissions
            if (linkText.contains(Parse.WHO_IS_HIRING) && !isOld(linkText)) {
                logger.info(String.format("Found relevant thread: %s (%s)", linkText, linkId));

                threads.add(new JobThread(linkId, linkText));
            }
        }
    }

    private void parseJobThreads() throws IOException {
        if (threads.isEmpty()) {
            logger.warning("No job threads found");
            return;
        }

        for (JobThread thread : threads) {
            parseJobThread(thread);
        }
    }

    private void parseJobThread(JobThread thread) throws IOException {
        logger.info(String.format("Parsing job thread %s", thread.text));

        // get all the html content from the thread
        String content = HttpClient.getUrl(URL.BASE_URL + thread.id);

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
        String id = link.substring(link.indexOf("=") + 1);

        // save everything neatly in a POJO so we can pass it to Gson when saving
        Job job = new Job();
        job.author = author;
        job.timestamp = DateUtil.getAbsoluteDate(linkAge).getTime();
        job.body = Jsoup.parse(body.toString()).text();
        job.bodyHtml = body.toString();

        // persist to elasticsearch
        saveJob(id, job);
    }

    private void saveJob(String id, Job job) {
        IndexResponse response = Main.getClient().prepareIndex(Index.HNJOBS, Type.JOB, id)
                .setSource(new Gson().toJson(job))
                .get();
    }

    /**
     * Parse the date from the submission title and return if the thread is too old or not
     *
     * @param title
     * @return if a thread is too old or not
     * @throws ParseException
     */
    private boolean isOld(String title) throws ParseException {
        String s = title.substring(title.indexOf("(") + 1, title.indexOf(")"));
        Date threadDate = sdf.parse(s);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -MONTHS_OLD_THRESHOLD);

        return threadDate.before(cal.getTime());
    }


    /**
     * remove job posts that are too old
     *
     * @see Parser#isOld(java.lang.String)
     */
    private void cleanOldJobs() {
        SearchResponse response = Main.getClient().prepareSearch(Index.HNJOBS)
                .setTypes(Type.JOB)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchAllQuery())
                .setPostFilter(QueryBuilders.rangeQuery(Field.TIMESTAMP).lt(getMonthsAgo(MONTHS_OLD_THRESHOLD)))
                .setExplain(false)
                .execute()
                .actionGet();

        // delete matching documents
        for (SearchHit hit : response.getHits()) {
            Main.getClient().prepareDelete(Index.HNJOBS, Type.JOB, hit.getId()).get();
        }
    }

    private long getMonthsAgo(int months) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -months);

        return cal.getTimeInMillis();
    }
}
