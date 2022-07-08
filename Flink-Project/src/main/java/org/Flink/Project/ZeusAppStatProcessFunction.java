package com.sling.dacoe.operators;

import com.sling.dacoe.schema.AppStatStatus;
import com.sling.dacoe.schema.ZeusAppRunStat;
import com.sling.dacoe.schema.ZeusEvent;
import com.sling.dacoe.utils.ZeusConstants;
import org.apache.flink.api.common.state.*;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("Duplicates")
public class ZeusAppStatProcessFunction extends KeyedProcessFunction<String, ZeusEvent, ZeusAppRunStat> implements ZeusConstants {


    // TODO: Find and remove the fileds which are not related to the usecase .
    private static final Logger log = LoggerFactory.getLogger(ZeusAppStatProcessFunction.class);
    private final long durationMs;
    private final long timeoutMs;
    private transient MapState<Long, String> windowState;
    private transient ListState<Tuple2<Long,String>> eventsTimelineState;
    private transient ValueState<ZeusAppRunStat> appRunStatState;
    private transient ValueState<AppStatStatus> status;
    private static final OutputTag<ZeusEvent> lateEvents = new OutputTag<>("lateEvents", TypeInformation.of(ZeusEvent.class));

    public ZeusAppStatProcessFunction(Time duration, Time timeoutMs) {
        this.durationMs = duration.toMilliseconds();
        this.timeoutMs = timeoutMs.toMilliseconds();
    }

    @Override
    public void open(Configuration parameters)  {
        //This is required to maintain the events in a window to decide the status of the emission.
        // The window is cleared for a time window when onTimer call back is invoked
        StateTtlConfig ttlConfig = StateTtlConfig.newBuilder(Time.hours(24))
                .updateTtlOnCreateAndWrite().neverReturnExpired().build();

        MapStateDescriptor<Long, String> appStatStatesDescriptor =
                new MapStateDescriptor<>("appStatStates", Long.class, String.class);
        appStatStatesDescriptor.enableTimeToLive(ttlConfig);
        windowState = getRuntimeContext().getMapState(appStatStatesDescriptor);

        ListStateDescriptor<Tuple2<Long,String>> eventsTimelineStateDescriptor =
                new ListStateDescriptor<>("eventsTimelineState", Types.TUPLE(Types.LONG,Types.STRING));
        eventsTimelineStateDescriptor.enableTimeToLive(ttlConfig);
        eventsTimelineState = getRuntimeContext().getListState(eventsTimelineStateDescriptor);

        ValueStateDescriptor<ZeusAppRunStat> stateDescriptor = new ValueStateDescriptor<>("appStat-state", ZeusAppRunStat.class);
        stateDescriptor.enableTimeToLive(ttlConfig);
        appRunStatState = getRuntimeContext().getState(stateDescriptor);

        ValueStateDescriptor<AppStatStatus> statusStateDescriptor = new ValueStateDescriptor<>("app-status-state", AppStatStatus.class);
        statusStateDescriptor.enableTimeToLive(ttlConfig);
        status = getRuntimeContext().getState(statusStateDescriptor);
    }

    @Override
    public void processElement(ZeusEvent event, KeyedProcessFunction<String, ZeusEvent, ZeusAppRunStat>.Context context, Collector<ZeusAppRunStat> collector) throws Exception {
        long receivedTime = (event.getReceivedAtInUtcMs() / 60000) * 60000;
        TimerService timerService = context.timerService();

        long endOfWindow = (receivedTime - (receivedTime % durationMs) + durationMs - 1);
        log.debug("EVENT ARRIVED ReceivedTime = {}, eventType = {}, endOfWindow = {}, Watermark = {}"
                , receivedTime, event.getEventTypeName(), endOfWindow, timerService.currentWatermark());
        timerService.registerEventTimeTimer(endOfWindow);

        // Window State
        String eventList = this.windowState.get(endOfWindow);
        if (eventList == null) {
            eventList = "";
        }
        eventList = eventList.concat(event.getEventTypeName()).concat(COMMA_DELIM);
        this.windowState.put(endOfWindow, eventList);
        this.eventsTimelineState.add(new Tuple2<>(event.getEventTime(),event.getEventTypeName()));

        ZeusAppRunStat currentState = this.appRunStatState.value();
        if (currentState == null) {
            currentState = new ZeusAppRunStat();
        }

        currentState.setDimensions(event);
        currentState.setMetrics(event);

        currentState.setLastModified(context.timestamp());
        this.appRunStatState.update(currentState);
    }

    @Override
    public void onTimer(long timestamp, KeyedProcessFunction<String, ZeusEvent, ZeusAppRunStat>.OnTimerContext ctx, Collector<ZeusAppRunStat> out) throws Exception {
        ZeusAppRunStat appRunStat = this.appRunStatState.value();

        String eventList = this.windowState.get(timestamp);
        AppStatStatus status = this.status.value();

        ArrayList<Tuple2<Long,String>> timelineList = new ArrayList<>();
        for (Tuple2<Long,String> inputTuple2: this.eventsTimelineState.get()) {
            timelineList.add(new Tuple2<>(inputTuple2.f0, inputTuple2.f1));
        }
        timelineList.sort((event1, event2) -> (int) (event1.f0 - event2.f0));

        appRunStat.setOpenandCrashCount(computeAppRunStats(timelineList));
        if (status != null && status.getPreviousLastModified() > 0) {

            //Check if session timed out with no events for timeoutMs
            if (ctx.timestamp() == status.getPreviousLastModified() + this.timeoutMs) {
                // if playback aggregate with ended status is already emitted, dont emit again
                if (status.isEmittedEnded()) {
                    windowState.remove(timestamp);
                    return;
                } else { // Expire the timed out session which has not ended yet.
                    appRunStat.setStatus(EXPIRED);
                    if(eventList != null) {
                        String[] events = eventList.split(",");
                        if (events.length == 1 && events[0].equals("AssetRequested")) {
                            appRunStat.setWaitTime(-1);
                        }
                    }
                }
                log.debug("REAL TIMEOUT onTimer Timeout Check timestamp = {},watermark = {}, lastModifiedTime = {} status = {}",
                        timestamp, ctx.timerService().currentWatermark(), appRunStat.getLastModified(), appRunStat.getStatus());
                appRunStat.computeFinalResult();
                out.collect(appRunStat);
                windowState.remove(timestamp);
                return;
            } else {
                // Delete the timeout timer since new events have come in before the timeout set based on previous last modified time.
                ctx.timerService().deleteEventTimeTimer(status.getPreviousLastModified() + this.timeoutMs);
                // register new timeout timer based on the current last modified
                ctx.timerService().registerEventTimeTimer(appRunStat.getLastModified() + this.timeoutMs);
            }
        }

        //this.setPlaybackStatus(appRunStat, eventList, status); // TODO: Do we need this to be done for App Stats?

        appRunStat.computeFinalResult();
        out.collect(appRunStat);

        log.debug("onTimer timestamp = {},watermark = {}, lastModifiedTime = {} status = {}",
                timestamp, ctx.timerService().currentWatermark(), appRunStat.getLastModified(), appRunStat.getStatus());

        this.windowState.remove(timestamp);
        this.appRunStatState.update(appRunStat);

    }

    private Map<String, Long> computeAppRunStats(ArrayList<Tuple2<Long,String>> timelineList){

        Map<String, Long > result = new HashMap<>();
        //double appcrashPercent =0;
        long appOpenCount = 0;
        long appCrashedCount = 0;

        for(Tuple2<Long, String> eventTimeLine : timelineList){
            if(APP_OPENEND_STR.equalsIgnoreCase(eventTimeLine.f1)){
                appOpenCount++;
            }
            else if(APP_CRASHED_STR.equalsIgnoreCase(eventTimeLine.f1)){
                appCrashedCount++;
            }
        }

        result.put(APP_OPENEND_STR, appOpenCount);
        result.put(APP_CRASHED_STR, appCrashedCount);

        return result;
    }



}
