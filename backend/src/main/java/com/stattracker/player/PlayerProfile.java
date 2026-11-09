package com.stattracker.player;

import com.stattracker.common.TeamRef;
import org.jspecify.annotations.Nullable;

public record PlayerProfile(
        long id,
        String fullName,
        String firstName,
        String lastName,
        @Nullable TeamRef team,
        @Nullable String position,
        @Nullable String jerseyNumber,
        @Nullable String height,
        @Nullable Integer weightLbs,
        @Nullable String country,
        @Nullable Integer draftYear,
        boolean active,
        @Nullable Integer fromYear,
        @Nullable Integer toYear) {
}
