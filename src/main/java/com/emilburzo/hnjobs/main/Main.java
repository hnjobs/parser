package com.emilburzo.hnjobs.main;


import com.emilburzo.hnjobs.parser.Parser;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

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

    private static void initEs() throws UnknownHostException {
        client = new PreBuiltTransportClient(Settings.EMPTY)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("hnjobs"), 9300));
    }

    public static TransportClient getClient() {
        return client;
    }
}
