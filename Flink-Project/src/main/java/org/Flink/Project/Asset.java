package com.sling.dacoe.schema;

public class Asset {

    private String assetId;
    private long duration;
    private String assetTitle;
    private Long anchorTime;

    private String contentType;
    private String episodeName;
    private long episodeNumber;
    private String seasonNumber;
    private String franchiseGuid;
    private String franchiseTitle;
    private String assetGenre;
    private String assetInfoUrl;
    private String programGuid;
    private String programType;
    private String publisherId;
    private String rating;
    private String assetUrl;
    private String usingDynamicAds;


    public Asset() {

    }

    public Long getAnchorTime() {
        return anchorTime;
    }

    public void setAnchorTime(Long anchorTime) {
        this.anchorTime = anchorTime;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getEpisodeName() {
        return episodeName;
    }

    public void setEpisodeName(String episodeName) {
        this.episodeName = episodeName;
    }

    public long getEpisodeNumber() { return episodeNumber; }

    public void setEpisodeNumber(long episodeNumber) { this.episodeNumber = episodeNumber; }

    public String getSeasonNumber() { return seasonNumber; }

    public void setSeasonNumber(String seasonNumber) { this.seasonNumber = seasonNumber; }

    public String getFranchiseGuid() {
        return franchiseGuid;
    }

    public void setFranchiseGuid(String franchiseGuid) {
        this.franchiseGuid = franchiseGuid;
    }

    public String getFranchiseTitle() {
        return franchiseTitle;
    }

    public void setFranchiseTitle(String franchiseTitle) {
        this.franchiseTitle = franchiseTitle;
    }

    public String getAssetGenre() {
        return assetGenre;
    }

    public void setAssetGenre(String assetGenre) {
        this.assetGenre = assetGenre;
    }

    public String getAssetInfoUrl() {
        return assetInfoUrl;
    }

    public void setAssetInfoUrl(String assetInfoUrl) {
        this.assetInfoUrl = assetInfoUrl;
    }

    public String getProgramGuid() {
        return programGuid;
    }

    public void setProgramGuid(String programGuid) {
        this.programGuid = programGuid;
    }

    public String getProgramType() {
        return programType;
    }

    public void setProgramType(String programType) {
        this.programType = programType;
    }

    public String getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(String publisherId) {
        this.publisherId = publisherId;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getAssetUrl() {
        return assetUrl;
    }

    public void setAssetUrl(String assetUrl) {
        this.assetUrl = assetUrl;
    }

    public String getUsingDynamicAds() {
        return usingDynamicAds;
    }

    public void setUsingDynamicAds(String usingDynamicAds) {
        this.usingDynamicAds = usingDynamicAds;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }


    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getAssetTitle() {
        return assetTitle;
    }

    public void setAssetTitle(String assetTitle) {
        this.assetTitle = assetTitle;
    }
}
