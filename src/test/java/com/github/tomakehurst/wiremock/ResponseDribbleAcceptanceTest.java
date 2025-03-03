/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock;

import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.testsupport.Assumptions.doNotRunOnMacOSXInCI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

public class ResponseDribbleAcceptanceTest {

    private static final int SOCKET_TIMEOUT_MILLISECONDS = 500;
    private static final int DOUBLE_THE_SOCKET_TIMEOUT = SOCKET_TIMEOUT_MILLISECONDS * 2;

    private static final byte[] BODY_BYTES = "the long sentence being sent".getBytes();

    public static final double TOLERANCE = 0.333; // Quite big, but this helps reduce CI failures

    @RegisterExtension
    public WireMockExtension wireMockRule = WireMockExtension.newInstance()
        .configureStaticDsl(true)
        .options(options().port(DYNAMIC_PORT).httpsPort(DYNAMIC_PORT))
        .build();

    private CloseableHttpClient httpClient;

    @BeforeEach
    public void init() throws IOException {
        stubFor(get("/warmup").willReturn(ok()));
        httpClient = HttpClientFactory.createClient(SOCKET_TIMEOUT_MILLISECONDS);
        // Warm up the server
        httpClient.execute(new HttpGet(wireMockRule.url("/warmup")));
    }

    @Test
    public void requestIsSuccessfulButTakesLongerThanSocketTimeoutWhenDribbleIsEnabled() throws Exception {
        doNotRunOnMacOSXInCI();

        stubFor(get("/delayedDribble").willReturn(
                ok()
                    .withBody(BODY_BYTES)
                    .withChunkedDribbleDelay(BODY_BYTES.length, DOUBLE_THE_SOCKET_TIMEOUT)));

        long start = System.currentTimeMillis();
        ClassicHttpResponse response = httpClient.execute(new HttpGet(wireMockRule.url("/delayedDribble")));
        byte[] responseBody = IOUtils.toByteArray(response.getEntity().getContent());
        int duration = (int) (System.currentTimeMillis() - start);

        assertThat(response.getCode(), is(200));
        assertThat(responseBody, is(BODY_BYTES));
        assertThat(duration, greaterThanOrEqualTo(SOCKET_TIMEOUT_MILLISECONDS));
        assertThat((double) duration, isWithinTolerance(DOUBLE_THE_SOCKET_TIMEOUT, TOLERANCE));
    }

    @Test
    public void servesAStringBodyInChunks() throws Exception {
        doNotRunOnMacOSXInCI();
        
        final int TOTAL_TIME = 500;

        stubFor(get("/delayedDribble").willReturn(
            ok()
                .withBody("Send this in many pieces please!!!")
                .withChunkedDribbleDelay(2, TOTAL_TIME)));

        long start = System.currentTimeMillis();
        ClassicHttpResponse response = httpClient.execute(new HttpGet(wireMockRule.url("/delayedDribble")));
        String responseBody = EntityUtils.toString(response.getEntity());
        double duration = (double) (System.currentTimeMillis() - start);

        assertThat(response.getCode(), is(200));
        assertThat(responseBody, is("Send this in many pieces please!!!"));
        assertThat(duration, isWithinTolerance(TOTAL_TIME, TOLERANCE));
    }

    @Test
    public void requestIsSuccessfulAndBelowSocketTimeoutWhenDribbleIsDisabled() throws Exception {
        doNotRunOnMacOSXInCI();

        stubFor(get("/nonDelayedDribble").willReturn(
                ok()
                    .withBody(BODY_BYTES)));

        long start = System.currentTimeMillis();
        ClassicHttpResponse response = httpClient.execute(new HttpGet(wireMockRule.url("/nonDelayedDribble")));
        byte[] responseBody = IOUtils.toByteArray(response.getEntity().getContent());
        int duration = (int) (System.currentTimeMillis() - start);

        assertThat(response.getCode(), is(200));
        assertThat(BODY_BYTES, is(responseBody));
        assertThat(duration, lessThan(SOCKET_TIMEOUT_MILLISECONDS));
    }

    private static Matcher<Double> isWithinTolerance(double value, double tolerance) {
        double maxDelta = value * tolerance;
        return closeTo(value, maxDelta);
    }
}