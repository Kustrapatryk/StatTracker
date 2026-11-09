package com.stattracker.common;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SeasonTest {

    @Test
    void formatsAndParsesIds() {
        assertThat(new Season(2025).id()).isEqualTo("2025-26");
        assertThat(new Season(1999).id()).isEqualTo("1999-00");
        assertThat(Season.parse("2003-04")).isEqualTo(new Season(2003));
        assertThat(Season.parse("2003-04").previous()).isEqualTo(Season.parse("2002-03"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025", "2025-27", "25-26", "2025/26", "", "1999-2000"})
    void rejectsMalformedIds(String id) {
        assertThatIllegalArgumentException().isThrownBy(() -> Season.parse(id));
    }

    @Test
    void currentSeasonStartsInOctoberUsTime() {
        assertThat(Season.current(clockAt("2026-11-06T12:00:00Z"))).isEqualTo(new Season(2026));
        assertThat(Season.current(clockAt("2026-06-20T12:00:00Z"))).isEqualTo(new Season(2025));
        assertThat(Season.current(clockAt("2026-10-01T03:00:00Z"))).as("still Sep 30 in New York")
                .isEqualTo(new Season(2025));
    }

    private static Clock clockAt(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }
}
