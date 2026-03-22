package com.emilburzo.hnjobs.main;


import com.emilburzo.hnjobs.parser.Parser;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class Main {

    private static final String ELASTICSEARCH_HOST = "ELASTICSEARCH_HOST";
    private static final String ELASTICSEARCH_PORT = "ELASTICSEARCH_PORT";

    private static RestHighLevelClient client;

    public static void main(String[] args) {
        try {
            initEs();

            new Parser();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void initEs() {
        String host = System.getenv().getOrDefault(ELASTICSEARCH_HOST, "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault(ELASTICSEARCH_PORT, "9200"));

        client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(host, port, "http"))
        );
    }

    public static RestHighLevelClient getClient() {
        return client;
    }
}
