package com.stattracker.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SearchNamesTest {

    @ParameterizedTest
    @CsvSource(quoteCharacter = '"', value = {
            "Nikola Jokić, nikola jokic",
            "Luka Dončić, luka doncic",
            "Kristaps Porziņģis, kristaps porzingis",
            "Alperen Şengün, alperen sengun",
            "Dennis Schröder, dennis schroder",
            "Przemysław Karnowski, przemyslaw karnowski",
            "De'Aaron Fox, de'aaron fox",
            "\"  Shai   Gilgeous-Alexander \", shai gilgeous-alexander"})
    void lowercasesAndStripsDiacritics(String name, String expected) {
        assertThat(SearchNames.normalize(name)).isEqualTo(expected);
    }
}
