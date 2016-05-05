package com.emilburzo.hnjobs.http;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class HttpClient {

    public static String getUrl(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = new OkHttpClient().newCall(request).execute();
        return response.body().string();
    }

}
