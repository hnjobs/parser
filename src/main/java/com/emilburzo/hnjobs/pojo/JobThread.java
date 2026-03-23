package com.emilburzo.hnjobs.pojo;

public record JobThread(Long id, String linkId, String text) {

    public JobThread(String linkId, String text) {
        this(Long.valueOf(linkId), linkId, text);
    }
}
