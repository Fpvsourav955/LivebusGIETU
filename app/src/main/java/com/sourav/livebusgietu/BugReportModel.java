package com.sourav.livebusgietu;

public class BugReportModel {
    private String name;
    private String email;
    private String message;
    private String imageUrl;

    public BugReportModel() {

    }

    public BugReportModel(String name, String email, String message, String imageUrl) {
        this.name = name;
        this.email = email;
        this.message = message;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
