package com.github.gcolin.platform;

public record MailAttachment(String filename, String contentType, byte[] content) {
    public MailAttachment {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename is required");
        }
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        if (content == null) {
            content = new byte[0];
        }
    }
}
