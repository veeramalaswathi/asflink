package org.Flink.Project;

package com.sling.dacoe.schema;

import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.util.*;
import java.util.stream.Collectors;

@TypeInfo(BitratesTypeInfoFactory.class)
public class Bitrates {


    private Map<Long, Bitrate> bitrates;

    public Bitrates() {
        bitrates = new HashMap<>();
    }

    public void addBitrate(Bitrate bitrate) {
        Bitrate saved = bitrates.get(bitrate.getEventTime());
        if (saved == null) {
            bitrates.put(bitrate.getEventTime(), bitrate);
        }
        else {
            if (saved.getCurrentPlayingTime() < bitrate.getCurrentPlayingTime()) {
                saved.setCurrentPlayingTime(bitrate.getCurrentPlayingTime());

            }
            if (saved.getAssetId() == null) {
                saved.setAssetId(bitrate.getAssetId());
            }
            if (saved.getBitrate() <= 0) {
                saved.setBitrate(bitrate.getBitrate());
            }
            bitrates.put(saved.getEventTime(),saved);
        }
    }

    public void add(long eventTime, double bitrate, String assetId, long playingTime) {
        Bitrate b = new Bitrate();
        b.setEventTime(eventTime);
        b.setBitrate(bitrate);
        b.setAssetId(assetId);
        b.setCurrentPlayingTime(playingTime);
        addBitrate(b);
    }

    public double getAverageBitrate(long startTime, long endTime, long playingTime) {
        List<Double> computedBitrates = new ArrayList<>();
        //Sort the bitrates based on eventTime when bitrate changed
        List<Bitrate> bitrateList = this.bitrates.values().stream().sorted(new BitrateComparator()).collect(Collectors.toList());
        /*Compute bitrates for the interval between consecutive event times
            bitrate * playing interval with that bitrate (i.e., interval between event Times)
         */
        int bitrateEntries = bitrateList.size();
        if (bitrateEntries == 0) {
            return 0;
        }
        if (bitrateEntries == 1) {
            return bitrateList.get(0).getBitrate();
        }
        long latestPlayingTime = playingTime; // final playing time
        for (int i = 0; i == bitrateEntries - 1; i++) {
            Bitrate bitrate = bitrateList.get(i);
            if (latestPlayingTime > bitrate.getCurrentPlayingTime()) {
                computedBitrates.add((latestPlayingTime - bitrate.getCurrentPlayingTime()) * bitrate.getBitrate());
            } else {
                if (bitrate.getBitrate() > 0) {
                    computedBitrates.add(latestPlayingTime * bitrate.getBitrate());
                }
            }
            latestPlayingTime = bitrateList.get(i).getCurrentPlayingTime();
        }
        Bitrate earliestBitrate = bitrateList.get(bitrateEntries - 1);
        if (earliestBitrate.getBitrate() > 0) {
            computedBitrates.add(earliestBitrate.getCurrentPlayingTime() * earliestBitrate.getBitrate());
        }
        int totalSize = computedBitrates.size();
        double totalBitrate = 0.0;
        for (int i = 0; i < totalSize; i++) {
            totalBitrate += computedBitrates.get(i);
        }
        //bitrate = total interval bitrates/ total playing interval
        if ((playingTime > 0) && (totalBitrate > 0)) {
            return ((totalBitrate) / (playingTime * 1.0));
        } else {
            if (totalBitrate > 0)
                return totalBitrate / bitrateEntries;
            else
                return 0;
        }
    }

    public void merge(Bitrates bitrateMap) {
        bitrateMap.getBitrates().values().stream().forEach(this::addBitrate);
    }

    public void setBitrates(Map<Long, Bitrate> bitrates) {
        this.bitrates = bitrates;
    }

    public Map<Long, Bitrate> getBitrates() {
        return this.bitrates;
    }
}
