package com.szymek.voicenote;

public class ListItem {
    private String author;
    private String title;
    private String date_and_duration;
    private String description;

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDateAndDuration(String date_and_duration) {
        this.date_and_duration = date_and_duration;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getDateAndDuration() {
        return date_and_duration;
    }

    public String getDescription() {
        return description;
    }
}
