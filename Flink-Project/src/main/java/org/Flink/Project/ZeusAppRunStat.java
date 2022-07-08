package com.sling.dacoe.schema;


import com.sling.dacoe.utils.Spi;
import com.sling.dacoe.utils.ZeusUtils;
import org.apache.flink.api.common.typeinfo.TypeInfo;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static com.sling.dacoe.utils.ZeusConstants.APP_CRASHED_STR;
import static com.sling.dacoe.utils.ZeusConstants.APP_OPENEND_STR;


public class ZeusAppRunStat {
    private long appCrashCount;
    private long appOpenCount;
    private double percentOfCrash;

    private String userId;
    private String sessionId;
    private String deviceId;
    private String playbackSessionId;

    //Software Information

    private String platform;
    private String platformId;
    private String playerVersion;
    private String appVersion;
    private String osType;
    private String osVersion;
    private String subscriptionPacks;
    private String browserName;
    private String browserVersion;
    private String playerFrameworkName;
    private String playerFrameworkVersion;


    //Device Information
    private Boolean debugBuild;
    private String deviceModel;
    private String deviceName;
    private String deviceType;
    private String deviceVendor;
    private String deviceModelYear;
    private String displayMode;


    //Network Information
    private String CDN;
    private String cdnName;
    private String IP;
    private String ISP;
    private String cdnHost;
    private String networkType;

    @JsonIgnore
    private Bitrates bitrates;

    @JsonIgnore
    private CDNList cdnList;

    private double bitrate;
    private double bandwidth;

    //Geo Information
    private String DMA;
    private String city;
    private String country;
    private String region;
    private String zipCode;

    //Status
    private String errorCode;
    private String errorPath;
    private boolean playbackSessionClosed;
    private int latest;
    private int status;
    private int severityId;
    private String errorMessage;
    private int httpStatusCode;
    private String severityName;

    @JsonIgnore
    private Errors errors;

    //Asset Information
    private Asset firstAsset;
    private Asset lastAsset;
    private String callSign;
    private String channelGenre;
    private String channelGuid;
    private String channelName;
    private String playType;
    private String contentType;
    private String sessionOrigin;

    //TimeInfo
    private long eventTime;
    private long startTime;
    private long endTime;
    private long requestTime;
    private long receivedTime;
    private long lastModified;
    private String timeZoneOffset;

    private String xservice;

    //Metrics
    private long playingTime;
    private long restartTime;
    private int restartTimeCount;
    private long initialBufferingTime;
    private long endedPlays;
    private long seekBufferingTime;
    private long startupTime;
    private int startupTimeCount;
    private long totalAbnormalBufferingTime;
    private long bufferingTime;
    private long maxTotalBufferingTime;
    private long maxTotalVideoPlayTime;
    private long minTotalBufferingTime;
    private long minTotalVideoPlayTime;
    private int attempt;
    private int play;
    private int ebvs;
    private int vsf;
    private int vpf;
    private int implicitVsf;
    private int implicitVpf;
    private long waitTime;

    private double averageRestartTime;

    private double averageStartupTime;

    //spi
    private double cirr;
    private long spiStream;
    private long spiBest;
    private long spiGood;
    private ArrayList<String> spiBestViolatorsList = new ArrayList<String>();
    private ArrayList<String> spiGoodViolatorsList = new ArrayList<String>();
    private int[] closedStatus = {5, 15, 125, 25};


    public double getAverageRestartTime() {
        return averageRestartTime;
    }

    public void setAverageRestartTime(double averageRestartTime) {
        this.averageRestartTime = averageRestartTime;
    }

    public double getAverageStartupTime() {
        return averageStartupTime;
    }

    public void setAverageStartupTime(double averageStartupTime) {
        this.averageStartupTime = averageStartupTime;
    }

    public static void setAsset(ZeusEvent event, Asset firstAsset) {
        firstAsset.setAssetId(event.getAssetGuid());
        firstAsset.setAssetTitle(event.getAssetTitle());
        firstAsset.setDuration(event.getAssetDuration());
        firstAsset.setAnchorTime(event.getAssetAnchorTime());
        firstAsset.setAssetGenre(event.getAssetGenre());
        firstAsset.setAssetInfoUrl(event.getAssetInfoUrl());
        firstAsset.setAssetUrl(event.getAssetURL());
        firstAsset.setContentType(event.getAssetContentType());
        firstAsset.setEpisodeName(event.getAssetEpisodeName());
        firstAsset.setEpisodeNumber(event.getAssetEpisodeNumber());
        firstAsset.setSeasonNumber(event.getSeasonNumber());
        firstAsset.setFranchiseGuid(event.getAssetFranchiseGuid());
        firstAsset.setFranchiseTitle(event.getAssetFranchiseTitle());
        firstAsset.setProgramGuid(event.getAssetProgramGuid());
        firstAsset.setProgramType(event.getAssetProgramType());
        firstAsset.setPublisherId(event.getAssetPublisherAssetID());
        firstAsset.setRating(event.getAssetRating());
        firstAsset.setUsingDynamicAds(event.getAssetUsingDynamicAds());
    }

    public void computeFinalResult_oldnotreq() {

        if(restartTimeCount > 0) {
            averageRestartTime = restartTime / restartTimeCount;
        }
        if (startupTimeCount > 0) {
            averageStartupTime = startupTime / startupTimeCount;
        }
        this.playingTime = this.playingTime - this.initialBufferingTime - this.seekBufferingTime - this.totalAbnormalBufferingTime;
        this.playingTime = this.playingTime > 0 ? this.playingTime : 0;
        this.bitrate = this.bitrates.getAverageBitrate(this.startTime, this.endTime, this.playingTime);

        //calculate SPI
        if (Arrays.stream(closedStatus).anyMatch(value -> value == this.status)) {
            if ((this.playingTime > 0) || (this.bufferingTime > 0)) {
                this.cirr = ((double) this.totalAbnormalBufferingTime / (double) (this.playingTime + this.bufferingTime));
            }
            if ((this.ebvs == 1) && (this.waitTime < 10000)) {
                this.setSpiStream(0);
            }
            else {
                this.setSpiStream(1);
            }

            this.spiBestViolatorsList = Spi.getSPIBestViolators(this.cirr, this.totalAbnormalBufferingTime,
                    this.vsf, this.vpf, this.averageStartupTime, this.ebvs,
                    this.waitTime, this.playingTime, this.bitrate, this.deviceType);

            this.spiGoodViolatorsList = Spi.getSPIGoodViolators(this.cirr, this.totalAbnormalBufferingTime,
                    this.vsf, this.vpf, this.averageStartupTime, this.ebvs,
                    this.waitTime, this.playingTime, this.bitrate, this.deviceType);

            this.spiBest = spiBestViolatorsList.size() == 0 ? 1:0;
            this.spiGood = spiGoodViolatorsList.size() == 0 ? 1:0;
        }
    }

    public long getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public double getAverageBitrate() {
        return bitrate;
    }

    public void setAverageBitrate(double bitrate) {
        this.bitrate = bitrate;
    }

    public Bitrates getBitrates() {
        return bitrates;
    }


    public void setBitrates(Bitrates bitrates) {
        this.bitrates = bitrates;
    }

    public CDNList getCdnList() {
        return cdnList;
    }

    public void setCdnList(CDNList cdnList) {
        this.cdnList = cdnList;
    }

    public void mergeBitrates(Bitrates bitrates) {
        this.bitrates.merge(bitrates);
    }

    public void mergeCDNs(CDNList cdnList) {
        this.cdnList.merge(cdnList);
    }

    public void mergeErrors(Errors errors) {
        this.errors.merge(errors.getErrors());
    }

    public Errors getErrors() {
        return errors;
    }

    public void setErrors(Errors errors) {
        this.errors = errors;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    public long getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(long receivedTime) {
        this.receivedTime = receivedTime;
    }

    public void setDimensions(ZeusEvent event) {

        // normalizing to the minute when event occurred.
        this.setEventTime(event.getEventTime());
        this.setReceivedTime(event.getReceivedAtInUtcMs());
        this.setUserId(event.getUserId());
        this.setSessionId(event.getEventSessionID());
        this.setPlaybackSessionId(event.getPlaybackSessionID());
        this.setAppVersion(event.getAppVersion());

        if ((event.getCdn() != null) && !(event.getCdn().equalsIgnoreCase("NA"))) {
            this.setCDN(event.getCdn());
            this.setCdnName(event.getCdnName());
        }
        this.setCity(event.getCity());
        this.setContentType(event.getAssetContentType());
        this.setCountry(event.getCountry());
        this.setDebugBuild(event.getDebugBuild());
        this.setDeviceId(event.getUniqueDeviceID());
        this.setDeviceModel(event.getDeviceModel());
        this.setDeviceName(event.getDeviceName());
        this.setDeviceType(event.getDeviceType());
        this.setDeviceVendor(event.getDeviceVendor());
        this.setIP(event.getIPAddress());
        this.setISP(event.getISPInformation());
        this.setDMA(event.getDMA());
        this.setZipCode(event.getZipCode());
        this.setOsType(event.getOSType());
        this.setOsVersion(event.getOSVersion());
        this.setAppVersion(event.getAppVersion());
        this.setPlayerVersion(event.getCSLVersion());
        this.setRegion(event.getRegion());
        this.setPlatform(event.getPlatform());
        this.setPlatformId(event.getPlatformID());
        if ((event.getSessionOrigin() != null) && !(event.getSessionOrigin().equalsIgnoreCase("NA"))) {
            this.setSessionOrigin(event.getSessionOrigin());
        }
        this.setNetworkType(event.getNetworkType());
        this.setErrorPath(event.getErrorPath());
        this.setSubscriptionPacks(event.getSubscriptionPacks());
        this.setDeviceModelYear(event.getDeviceModelYear());
        this.setDeviceDisplayMode(event.getDeviceDisplayMode());
        this.setBrowserVersion(event.getSoftwareBrowserVersion());
        this.setPlayerFrameworkName(event.getPlayerFrameworkName());
        this.setPlayerFrameworkVersion(event.getPlayerFrameworkVersion());
        this.setBrowserName(event.getSoftwareBrowserName());
        this.setChannelGuid(event.getAssetChannelGuid());
        this.setChannelName(event.getAssetChannelName());
        this.setCallSign(event.getAssetCallSign());
        this.setChannelGenre(event.getAssetChannelGenre());
        this.setContentType(event.getAssetContentType());
        this.setPlayType(event.getPlayType());
        this.setTimeZoneOffset(event.getTimeZoneOffset());
        this.setXservice(event.getxService());


        if (firstAsset.getAssetTitle() == null || firstAsset.getAssetTitle().equals("NA")) {
            setAsset(event, firstAsset);
        }

        if ((event.getAssetTitle() != null) && !(event.getAssetTitle().equals("NA"))) {
            setAsset(event, lastAsset);
        }

        if (event.getEventTypeName().equals("AppError") && (event.getSeverityName().equalsIgnoreCase("fatal") || event.getSeverityName().equalsIgnoreCase("policyviolation") || event.getSeverityName().equalsIgnoreCase("policy violation"))) {
            this.setErrorPath(event.getErrorPath());
            this.setErrorCode(event.getErrorTypeName());
            this.setSeverityId(event.getSeverityId());
            this.setErrorMessage(event.getErrorMessage());
            this.setHttpStatusCode(event.getHttpStatusCode());
            this.setSeverityName(event.getSeverityName());
        }

        if (event.getEventTypeName().equalsIgnoreCase("PlaybackSessionClosed")) {
            this.setWaitTime(event.getWaitTime());
            this.setPlaybackSessionClosed(true);
        }


    }


    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public int getEbvs() {
        return ebvs;
    }

    public void setEbvs(int ebvs) {
        this.ebvs = ebvs;
    }

    public int getVsf() {
        return vsf;
    }

    public void setVsf(int vsf) {
        this.vsf = vsf;
    }

    public int getVpf() {
        return vpf;
    }

    public void setVpf(int vpf) {
        this.vpf = vpf;
    }

    public Asset getFirstAsset() {
        return firstAsset;
    }

    public void setFirstAsset(Asset firstAsset) {
        this.firstAsset = firstAsset;
    }

    public Asset getLastAsset() {
        return lastAsset;
    }

    public void setLastAsset(Asset lastAsset) {
        this.lastAsset = lastAsset;
    }

    public void setMetrics(ZeusEvent event) {

        // Attempt
        this.setAttempt(1);
        //Play

        if (this.getPlay() != 1) {
            if (ZeusUtils.hasPlayStarted(event)) {
                this.setPlay(1);
            }
        }

        // EBVS, VSF, VPF

        if (event.getEventTypeName().equals("PlaybackSessionClosed")) {
            switch (event.getClosedType()) {
                case "ExitBeforeVideoStart":
                    this.ebvs = 1;
                    this.waitTime = event.getWaitTime();
                    break;
                case "VideoStartFailure":
                    this.vsf = 1;
                    break;
                case "VideoPlaybackFailure":
                    this.vpf = 1;
                    break;
                default:
                    break;
            }
        }

        // Fallback logic for VPF, VSF & EBVS

        if (event.getEventTypeName().equals("AppError") && (event.getSeverityName().equalsIgnoreCase("fatal") || event.getSeverityName().equalsIgnoreCase("policyviolation") || event.getSeverityName().equalsIgnoreCase("policy violation"))) {
            if (this.playingTime > 0) {
                this.vpf = 1;
                this.implicitVpf = 1;
            } else {
                this.vsf = 1;
                this.implicitVsf = 1;
            }
            this.setPlaybackSessionClosed(true);
        }

        //Buffering Time

        if (this.requestTime == 0 || this.requestTime > event.getEventTime()) {
            this.requestTime = event.getEventTime();
        }

        if ((event.getEventTime() > 0) && ((this.startTime == 0) || (event.getEventTime() < this.startTime))) {
            this.startTime = event.getEventTime();
        }
        if ((event.getEventTime() > 0) && (this.endTime < event.getEventTime())) {
            this.endTime = event.getEventTime();
        }


        if (event.getEventTypeName().equals("BufferingEnded")) {
            //Initial Buffering
            /*
                Check the timestamp of buffering ended event,
                if the eventTime of buffering ended is within one min of startTime, then initial buffering
                else abnormal buffering (if seek is false)
             */
            if (event.getSeekInducedBuffering()) {
                this.seekBufferingTime += event.getDuration();
            } else {
                if (play <= 0) { // Asset Started has not happened hence initial buffering
                    this.initialBufferingTime += event.getDuration();
                } else {
                    this.totalAbnormalBufferingTime += event.getDuration();
                }
            }
            if (this.maxTotalBufferingTime < event.getTotalBufferTime()) {
                this.maxTotalBufferingTime = event.getTotalBufferTime();
            }
            if (this.minTotalBufferingTime > event.getTotalBufferTime()) {
                this.minTotalBufferingTime = event.getTotalBufferTime();
            }
        }

        //Video Restart Time and Counts

        if (event.getEventTypeName().equals("BufferingEnded")) {
            if (event.getSeekInducedBuffering()) {
                this.restartTimeCount += 1;
                this.restartTime = this.seekBufferingTime;
            }
        }

        if (event.getEventTypeName().equalsIgnoreCase("AssetStarted") && (event.getStartupDuration() >= 0L)) {
            this.startupTime += event.getStartupDuration();
            this.startupTimeCount += 1;
        }

        this.latest = latest + 1;
        //Bitrate - Capture the current bitrate and time.
        if (event.getBitrate() > 0) {
            bitrates.add(event.getEventTime(), event.getBitrate(), event.getAssetGuid(), event.getTotalVideoPlaytime());
        }

    }

    public double getBitrate() {
        return bitrate;
    }

    public void setBitrate(double bitrate) {
        this.bitrate = bitrate;
    }

    public String getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(String displayMode) {
        this.displayMode = displayMode;
    }

    public String getBrowserName() {
        return browserName;
    }

    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
    }

    public String getCdnName() {
        return cdnName;
    }

    public void setCdnName(String cdnName) {
        if (cdnName == null || cdnName.isEmpty())
            return;
        this.cdnName = cdnName;

    }

    public String getCDN() {
        return CDN;
    }

    public void setCDN(String CDN) {
        if (CDN == null || CDN.isEmpty()) return;
        this.CDN = CDN;
    }

    public String getDMA() {
        return DMA;
    }

    public void setDMA(String DMA) {
        if (DMA == null || DMA.isEmpty()) return;
        this.DMA = DMA;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        if (IP == null || IP.isEmpty()) return;
        this.IP = IP;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getPlayerFrameworkVersion() {
        return playerFrameworkVersion;
    }

    public void setPlayerFrameworkVersion(String playerFrameworkVersion) {
        this.playerFrameworkVersion = playerFrameworkVersion;
    }

    public String getErrorPath() {
        return errorPath;
    }

    public void setErrorPath(String errorPath) {
        this.errorPath = errorPath;
    }

    public String getCallSign() {
        return callSign;
    }

    public void setCallSign(String callSign) {
        this.callSign = callSign;
    }

    public String getChannelGenre() {
        return channelGenre;
    }

    public void setChannelGenre(String channelGenre) {
        this.channelGenre = channelGenre;
    }

    public String getChannelGuid() {
        return channelGuid;
    }

    public void setChannelGuid(String channelGuid) {
        this.channelGuid = channelGuid;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getISP() {

        return ISP;
    }

    public void setISP(String ISP) {
        if (ISP == null || ISP.isEmpty()) return;
        this.ISP = ISP;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        if (appVersion == null || appVersion.isEmpty()) return;
        this.appVersion = appVersion;
    }


    public double getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    public long getBufferingTime() {
        return bufferingTime;
    }

    public void setBufferingTime(long bufferingTime) {
        this.bufferingTime = bufferingTime;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        if (city == null || city.isEmpty()) return;
        this.city = city;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) return;
        this.contentType = contentType;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        if (country == null || country.isEmpty()) return;
        this.country = country;
    }

    public boolean isDebugBuild() {
        return debugBuild;
    }

    public void setDebugBuild(boolean debugBuild) {
        this.debugBuild = debugBuild;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) return;
        this.deviceId = deviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        if (deviceModel == null || deviceModel.isEmpty()) return;
        this.deviceModel = deviceModel;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        if (deviceName == null || deviceName.isEmpty()) return;
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        if (deviceType == null || deviceType.isEmpty()) return;
        this.deviceType = deviceType;
    }

    public String getDeviceVendor() {
        return deviceVendor;
    }

    public void setDeviceVendor(String deviceVendor) {
        if (deviceVendor == null || deviceVendor.isEmpty()) return;
        this.deviceVendor = deviceVendor;
    }


    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getEndedPlays() {
        return endedPlays;
    }

    public void setEndedPlays(long endedPlays) {
        this.endedPlays = endedPlays;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isEmpty()) return;
        this.errorCode = errorCode;
    }

    public long getInitialBufferingTime() {
        return initialBufferingTime;
    }

    public void setInitialBufferingTime(long initialBufferingTime) {
        this.initialBufferingTime = initialBufferingTime;
    }

    public boolean isPlaybackSessionClosed() {
        return playbackSessionClosed;
    }

    public void setPlaybackSessionClosed(boolean playbackSessionClosed) {
        this.playbackSessionClosed = playbackSessionClosed;
    }

    public int getLatest() {
        return latest;
    }

    public void setLatest(int latest) {
        this.latest = latest;
    }

    public long getMaxTotalBufferingTime() {
        return maxTotalBufferingTime;
    }

    public void setMaxTotalBufferingTime(long maxTotalBufferingTime) {
        this.maxTotalBufferingTime = maxTotalBufferingTime;
    }

    public long getMaxTotalVideoPlayTime() {
        return maxTotalVideoPlayTime;
    }

    public void setMaxTotalVideoPlayTime(long maxTotalVideoPlayTime) {
        this.maxTotalVideoPlayTime = maxTotalVideoPlayTime;
    }

    public long getMinTotalBufferingTime() {
        return minTotalBufferingTime;
    }

    public void setMinTotalBufferingTime(long minTotalBufferingTime) {
        this.minTotalBufferingTime = minTotalBufferingTime;
    }

    public long getMinTotalVideoPlayTime() {
        return minTotalVideoPlayTime;
    }

    public void setMinTotalVideoPlayTime(long minTotalVideoPlayTime) {
        this.minTotalVideoPlayTime = minTotalVideoPlayTime;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        if (osType == null || osType.isEmpty()) return;
        this.osType = osType;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        if (osVersion == null || osVersion.isEmpty()) return;
        this.osVersion = osVersion;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        if (platform == null || platform.isEmpty()) return;
        this.platform = platform;
    }

    public int getPlay() {
        return play;
    }

    public void setPlay(int play) {
        this.play = play;
    }

    public String getPlayType() {
        return playType;
    }

    public void setPlayType(String playType) {
        if (playType == null || playType.isEmpty()) return;
        this.playType = playType;
    }

    public String getPlaybackSessionId() {
        return playbackSessionId;
    }

    public void setPlaybackSessionId(String playbackSessionId) {

        this.playbackSessionId = playbackSessionId;
    }

    public String getPlayerVersion() {
        return playerVersion;
    }

    public void setPlayerVersion(String playerVersion) {
        if (playerVersion == null || playerVersion.isEmpty()) return;
        this.playerVersion = playerVersion;
    }

    public long getPlayingTime() {
        return playingTime;
    }

    public void setPlayingTime(long playingTime) {
        this.playingTime = playingTime;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        if (region == null || region.isEmpty()) return;
        this.region = region;
    }

    public long getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(long requestTime) {
        this.requestTime = requestTime;
    }

    public long getRestartTime() {
        return restartTime;
    }

    public void setRestartTime(long restartTime) {
        this.restartTime = restartTime;
    }

    public int getRestartTimeCount() {
        return restartTimeCount;
    }

    public void setRestartTimeCount(int restartTimeCount) {
        this.restartTimeCount = restartTimeCount;
    }

    public long getSeekBufferingTime() {
        return seekBufferingTime;
    }

    public void setSeekBufferingTime(long seekBufferingTime) {
        this.seekBufferingTime = seekBufferingTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionOrigin() {
        return sessionOrigin;
    }

    public void setSessionOrigin(String sessionOrigin) {
        if (sessionOrigin == null || sessionOrigin.isEmpty()) return;
        this.sessionOrigin = sessionOrigin;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartupTime() {
        return startupTime;
    }

    public void setStartupTime(long startupTime) {
        this.startupTime = startupTime;
    }

    public int getStartupTimeCount() {
        return startupTimeCount;
    }

    public void setStartupTimeCount(int startupTimeCount) {
        this.startupTimeCount = startupTimeCount;
    }

    public int getStatus() {
        return status;
    }


    public void setStatus(int status) {
        this.status = status;
    }

    public long getTotalAbnormalBufferingTime() {
        return totalAbnormalBufferingTime;
    }

    public void setTotalAbnormalBufferingTime(long totalAbnormalBufferingTime) {
        this.totalAbnormalBufferingTime = totalAbnormalBufferingTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        if (zipCode == null || zipCode.isEmpty()) return;
        this.zipCode = zipCode;
    }

    public String getCdnHost() {
        return cdnHost;
    }

    public void setCdnHost(String cdnHost) {
        this.cdnHost = cdnHost;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public String getSubscriptionPacks() {
        return subscriptionPacks;
    }

    public void setSubscriptionPacks(String subscriptionPacks) {
        this.subscriptionPacks = subscriptionPacks;
    }

    public String getDeviceModelYear() {
        return deviceModelYear;
    }

    public void setDeviceModelYear(String deviceModelYear) {
        this.deviceModelYear = deviceModelYear;
    }

    public String getDeviceDisplayMode() {
        return displayMode;
    }

    public void setDeviceDisplayMode(String displayMode) {
        this.displayMode = displayMode;
    }

    public String getPlayerFrameworkName() {
        return playerFrameworkName;
    }

    public void setPlayerFrameworkName(String playerFrameworkName) {
        this.playerFrameworkName = playerFrameworkName;
    }

    //SPI
    public double getCirr() {
        return cirr;
    }

    public void setCirr(double cirr) {
        this.cirr = cirr;
    }

    public void setSpiStream(long spiStream) {
        this.spiStream = spiStream;
    }

    public long getSpiStream() {
        return spiStream;
    }

    public void setSpiBest(long spiBest) {
        this.spiBest = spiBest;
    }

    public long getSpiBest() {
        return spiBest;
    }

    public void setSpiGood(long spiGood) {
        this.spiGood = spiGood;
    }

    public long getSpiGood() {
        return spiGood;
    }

    public void setSpiBestViolatorsList(ArrayList<String> spiBestViolatorsList) {
        this.spiBestViolatorsList = spiBestViolatorsList;
    }

    public ArrayList<String> getSpiBestViolatorsList() {
        return spiBestViolatorsList;
    }

    public void setSpiGoodViolatorsList(ArrayList<String> spiGoodViolatorsList) {
        this.spiGoodViolatorsList = spiGoodViolatorsList;
    }

    public ArrayList<String> getSpiGoodViolatorsList() {
        return spiGoodViolatorsList;
    }

    public int getSeverityId() { return severityId; }

    public void setSeverityId(int severityId) { this.severityId = severityId; }

    public String getTimeZoneOffset() { return timeZoneOffset; }

    public void setTimeZoneOffset(String timeZoneOffset) { this.timeZoneOffset = timeZoneOffset; }

    public String getXservice() { return xservice; }

    public void setXservice(String xservice) { this.xservice = xservice; }

    public String getErrorMessage() { return errorMessage; }

    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getHttpStatusCode() { return httpStatusCode; }

    public void setHttpStatusCode(int httpStatusCode) { this.httpStatusCode = httpStatusCode; }

    public int getImplicitVsf() { return implicitVsf; }

    public void setImplicitVsf(int implicitVsf) { this.implicitVsf = implicitVsf; }

    public int getImplicitVpf() { return implicitVpf; }

    public void setImplicitVpf(int implicitVpf) { this.implicitVpf = implicitVpf; }

    public String getSeverityName() { return severityName; }

    public void setSeverityName(String severityName) { this.severityName = severityName; }

    public ZeusAppRunStat(){
        this.appCrashCount = 0;
        this.appOpenCount = 0 ;
        bitrates = new Bitrates();
        firstAsset = new Asset();
        lastAsset = new Asset();
        cdnList = new CDNList();
        errors = new Errors();
        this.restartTime = 0;
        this.restartTimeCount = 0;
        this.seekBufferingTime = 0;
        this.startupTime = 0;
        this.startupTimeCount = 0;
        this.latest = 0;
        averageRestartTime = 0;
        averageStartupTime = 0;
    }
    /*public ZeusAppRunStat(long appCrashCount, long appOpenCount, double percentOfCrash) {
        this.appCrashCount = appCrashCount;
        this.appOpenCount = appOpenCount;
        this.percentOfCrash = percentOfCrash;
    }*/

    public long getAppCrashCount() {
        return appCrashCount;
    }

    public void setAppCrashCount(long appCrashCount) {
        this.appCrashCount = appCrashCount;
    }

    public long getAppOpenCount() {
        return appOpenCount;
    }

    public void setAppOpenCount(long appOpenCount) {
        this.appOpenCount = appOpenCount;
    }

    public double getPercentOfCrash() {
        return percentOfCrash;
    }

    public void setPercentOfCrash(double percentOfCrash) {
        this.percentOfCrash = percentOfCrash;
    }

    public void setOpenandCrashCount(Map<String,Long> map){
        setAppCrashCount(map.get(APP_CRASHED_STR));
        setAppOpenCount(map.get(APP_OPENEND_STR));
    }
/*
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZeusAppRunStat that = (ZeusAppRunStat) o;
        return appCrashCount == that.appCrashCount && appOpenCount == that.appOpenCount
                && Double.compare(that.percentOfCrash, percentOfCrash) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appCrashCount, appOpenCount, percentOfCrash);
    }*/


    public void computeFinalResult(){
        if(appOpenCount > 0){
            percentOfCrash = appCrashCount / appOpenCount * 100 ;
        }
        else {
            percentOfCrash = 0;
        }

    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZeusAppRunStat appStat = (ZeusAppRunStat) o;
        return appOpenCount == appStat.appOpenCount && appCrashCount == appStat.appCrashCount && Objects.equals(userId, appStat.userId) &&
                Objects.equals(sessionId, appStat.sessionId) && Objects.equals(deviceId, appStat.deviceId) &&
                Objects.equals(playbackSessionId, appStat.playbackSessionId) && Objects.equals(platform, appStat.platform) &&
                Objects.equals(status, appStat.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appOpenCount, appCrashCount, userId, sessionId, deviceId, playbackSessionId, platform, status, playingTime);
    }

}
