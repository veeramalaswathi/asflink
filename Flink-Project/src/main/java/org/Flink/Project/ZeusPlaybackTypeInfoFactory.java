package org.Flink.Project;

import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ZeusPlaybackTypeInfoFactory extends TypeInfoFactory<ZeusPlayback> {
    @Override
    public TypeInformation<ZeusPlayback> createTypeInfo(Type type, Map<String, TypeInformation<?>> map) {
        Map<String, TypeInformation<?>> fields =
                new HashMap<String, TypeInformation<?>>() {
                    {
                        put("userId", Types.STRING);
                        put("sessionId", Types.STRING);
                        put("deviceId", Types.STRING);
                        put("playbackSessionId", Types.STRING);
                        put("platform", Types.STRING);
                        put("platformId", Types.STRING);
                        put("playerVersion", Types.STRING);
                        put("appVersion", Types.STRING);
                        put("osType", Types.STRING);
                        put("osVersion", Types.STRING);
                        put("subscriptionPacks", Types.STRING);
                        put("browserName", Types.STRING);
                        put("browserVersion", Types.STRING);
                        put("playerFrameworkName", Types.STRING);
                        put("playerFrameworkVersion", Types.STRING);
                        put("debugBuild", Types.BOOLEAN);
                        put("deviceModel", Types.STRING);
                        put("deviceName", Types.STRING);
                        put("deviceType", Types.STRING);
                        put("deviceVendor", Types.STRING);
                        put("deviceModelYear", Types.STRING);
                        put("displayMode", Types.STRING);
                        put("CDN", Types.STRING);
                        put("cdnName", Types.STRING);
                        put("IP", Types.STRING);
                        put("ISP", Types.STRING);
                        put("cdnHost", Types.STRING);
                        put("networkType", Types.STRING);
                        put("bitrate", Types.DOUBLE);
                        put("bandwidth", Types.DOUBLE);
                        put("DMA", Types.STRING);
                        put("city", Types.STRING);
                        put("country", Types.STRING);
                        put("region", Types.STRING);
                        put("zipCode", Types.STRING);
                        put("errorCode", Types.STRING);
                        put("errorPath", Types.STRING);
                        put("playbackSessionClosed", Types.BOOLEAN);
                        put("latest", Types.INT);
                        put("status", Types.INT);
                        put("callSign", Types.STRING);
                        put("channelGenre", Types.STRING);
                        put("channelGuid", Types.STRING);
                        put("channelName", Types.STRING);
                        put("playType", Types.STRING);
                        put("contentType", Types.STRING);
                        put("sessionOrigin", Types.STRING);
                        put("eventTime", Types.LONG);
                        put("startTime", Types.LONG);
                        put("endTime", Types.LONG);
                        put("requestTime", Types.LONG);
                        put("receivedTime", Types.LONG);
                        put("lastModified", Types.LONG);
                        put("playingTime", Types.LONG);
                        put("restartTime", Types.LONG);
                        put("restartTimeCount", Types.INT);
                        put("initialBufferingTime", Types.LONG);
                        put("endedPlays", Types.LONG);
                        put("seekBufferingTime", Types.LONG);
                        put("startupTime", Types.LONG);
                        put("startupTimeCount", Types.INT);
                        put("totalAbnormalBufferingTime", Types.LONG);
                        put("bufferingTime", Types.LONG);
                        put("maxTotalBufferingTime", Types.LONG);
                        put("maxTotalVideoPlayTime", Types.LONG);
                        put("minTotalBufferingTime", Types.LONG);
                        put("minTotalVideoPlayTime", Types.LONG);
                        put("attempt", Types.INT);
                        put("play", Types.INT);
                        put("ebvs", Types.INT);
                        put("vsf", Types.INT);
                        put("vpf", Types.INT);
                        put("waitTime", Types.LONG);
                        put("averageStartupTime",Types.DOUBLE);
                        put("averageRestartTime",Types.DOUBLE);
                        put("cirr",Types.DOUBLE);
                        put("spiStream", Types.LONG);
                        put("spiBest", Types.LONG);
                        put("spiGood", Types.LONG);
                        put("spiBestViolatorsList", Types.LIST(Types.STRING));
                        put("spiGoodViolatorsList", Types.LIST(Types.STRING));
                        put("closedStatus", Types.PRIMITIVE_ARRAY(Types.INT));
                        put("severityId", Types.INT);
                        put("timeZoneOffset", Types.STRING);
                        put("xservice", Types.STRING);
                        put("errorMessage", Types.STRING);
                        put("httpStatusCode", Types.INT);
                        put("implicitVsf", Types.INT);
                        put("implicitVpf", Types.INT);
                        put("severityName", Types.STRING);
                    }
                };
        return Types.POJO(ZeusPlayback.class, fields);
    }
}
