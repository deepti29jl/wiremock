package com.github.tomakehurst.wiremock.matching;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.DateTimeOffset;
import com.github.tomakehurst.wiremock.common.DateTimeTruncation;
import com.github.tomakehurst.wiremock.common.Json;
import org.junit.Test;

import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EqualToDateTimePatternTest {

    @Test
    public void matchesZonedToZoned() {
        StringValuePattern matcher = WireMock.equalToDateTime("2021-06-14T12:13:14Z");

        assertTrue(matcher.match("2021-06-14T12:13:14Z").isExactMatch());
        assertFalse(matcher.match("1921-06-14T12:13:14Z").isExactMatch());
    }

    @Test
    public void matchesLocalToLocal() {
        StringValuePattern matcher = WireMock.equalToDateTime("2021-06-14T12:13:14");

        assertTrue(matcher.match("2021-06-14T12:13:14").isExactMatch());
        assertFalse(matcher.match("1921-06-14T12:13:14").isExactMatch());
    }

    @Test
    public void matchesLocalToZoned() {
        StringValuePattern matcher = WireMock.equalToDateTime("2021-06-14T12:13:14");

        assertTrue(matcher.match("2021-06-14T12:13:14Z").isExactMatch());
        assertFalse(matcher.match("1921-06-14T12:13:14Z").isExactMatch());
    }

    @Test
    public void returnsAReasonableDistanceWhenNoMatchForLocalExpectedZonedActual() {
        StringValuePattern matcher = WireMock.equalToDateTime("2021-01-01T00:00:00Z");
        assertThat(matcher.match("2071-01-01T00:00:00Z").getDistance(), is(0.5));
        assertThat(matcher.match("2121-01-01T00:00:00Z").getDistance(), is(1.0));
        assertThat(matcher.match("2022-01-01T00:00:00Z").getDistance(), is(0.01));
    }

    @Test
    public void serialisesToJson() {
        EqualToDateTimePattern matcher = new EqualToDateTimePattern(
                DateTimeOffset.fromString("now -5 days"),
                DateTimeTruncation.LAST_DAY_OF_MONTH,
                DateTimeTruncation.FIRST_DAY_OF_YEAR
        );

        assertThat(Json.write(matcher), jsonEquals("{\n" +
                "  \"equalToDateTime\": \"now -5 days\",\n" +
                "  \"truncateExpected\": \"last day of month\",\n" +
                "  \"truncateActual\": \"first day of year\"\n" +
                "}"));
    }

    @Test
    public void deserialisesFromJson() {
        StringValuePattern matcher = Json.read("{\n" +
                "  \"equalToDateTime\": \"now\",\n" +
                "  \"truncateExpected\": \"first hour of day\",\n" +
                "  \"truncateActual\": \"first hour of day\"\n" +
                "}", EqualToDateTimePattern.class);

        ZonedDateTime good = ZonedDateTime.now().truncatedTo(DAYS);
        ZonedDateTime bad = ZonedDateTime.now().truncatedTo(DAYS).minus(5, HOURS);

        assertTrue(matcher.match(good.toString()).isExactMatch());
        assertFalse(matcher.match(bad.toString()).isExactMatch());
    }
}
