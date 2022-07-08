package com.sling.dacoe.utils;

public interface ZeusConstants {


    // attempted = 1, started = 2, active = 3, not_started = 0, ended = 5,
    // expired = 6, attempt.start = 12, attempt.start.end = 125
    String COMMA_DELIM = ",";
    int ATTEMPTED = 1;
    int STARTED = 2;
    int STARTED_ENDED = 25;
    int ACTIVE = 3;
    int NOT_STARTED = 0;
    int ENDED = 5;
    int ATTEMPTED_ENDED = 15;
    int EXPIRED = 6;
    int ATTEMPTED_STARTED = 12;
    int ATTEMPTED_STARTED_ENDED = 125;

    int LAST = 99;


    // Application Stats
    String APP_CRASHED_STR = "AppCrashed";
    String APP_OPENEND_STR = "AppOpened";
}
