package com.emilburzo.hnjobs.pojo;

public record Job(String author, Long timestamp, Long src, String body, String bodyHtml) {
}
