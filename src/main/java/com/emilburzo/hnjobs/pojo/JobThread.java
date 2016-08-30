package com.emilburzo.hnjobs.pojo;

public class JobThread {

    public Long id;
    public String linkId;
    public String text;

    public JobThread(String linkId, String text) {
        this.linkId = linkId;
        this.text = text;
        this.id = Long.valueOf(linkId.substring(linkId.indexOf("=") + 1));
    }
}
