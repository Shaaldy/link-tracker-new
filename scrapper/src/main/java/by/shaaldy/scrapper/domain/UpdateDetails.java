package by.shaaldy.scrapper.domain;

import java.time.Instant;

public record UpdateDetails(String title, String author, Instant createdAt, String preview) {}
