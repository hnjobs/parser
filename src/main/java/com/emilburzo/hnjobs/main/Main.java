package com.emilburzo.hnjobs.main;


import com.emilburzo.hnjobs.parser.Parser;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    private static final String ELASTICSEARCH_HOST = "ELASTICSEARCH_HOST";
    private static final String ELASTICSEARCH_PORT = "ELASTICSEARCH_PORT";

    private static TransportClient client;

    public static void main(String[] args) {
        try {
            initEs();

            new Parser();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.close();
        }
    }

    private void initEs() throws UnknownHostException {
        client = TransportClient.builder().build().addTransportAddress(new InetSocketTransportAddress(
                InetAddress.getByName(System.getenv().getOrDefault(ELASTICSEARCH_HOST, "hnjobs")),
                Integer.parseInt(System.getenv().getOrDefault(ELASTICSEARCH_PORT, "9300")))
        );
    }

    public static TransportClient getClient() {
        return client;
    }
}
