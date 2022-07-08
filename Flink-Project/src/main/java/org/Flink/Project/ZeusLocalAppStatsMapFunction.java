package com.sling.dacoe.operators;

import com.sling.dacoe.schema.ZeusAppRunStat;
import com.sling.dacoe.schema.ZeusPlayback;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZeusLocalAppStatsMapFunction  implements MapFunction<ZeusAppRunStat, String> {
    private static final Logger log = LoggerFactory.getLogger(ZeusLocalAppStatsMapFunction.class);

    @Override
    public String map(ZeusAppRunStat zeusAppRunStat) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        log.debug("Converting playback to json for final sink write : {}",zeusAppRunStat.getPlaybackSessionId());
        return  mapper.writeValueAsString(zeusAppRunStat);
    }
}
