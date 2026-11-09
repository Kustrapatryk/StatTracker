package com.stattracker.common;

/** Phase of an NBA season. Preseason and the NBA Cup final do not count towards season stats and are not stored. */
public enum SeasonType {
    REGULAR,
    PLAY_IN,
    PLAYOFFS
}
