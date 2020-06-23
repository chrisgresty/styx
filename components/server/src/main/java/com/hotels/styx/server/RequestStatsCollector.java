/*
  Copyright (C) 2013-2020 Expedia Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.hotels.styx.server;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.hotels.styx.api.Metrics.name;
import static java.lang.String.valueOf;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * An implementation of request event sink that maintains Styx request statistics.
 */
public class RequestStatsCollector implements RequestProgressListener {

    public static final String STATUS_TAG = "statusCode";
    public static final String STATUS_CLASS_TAG = "statusClass";

    public static final String REQUEST_OUTSTANDING = "request.outstanding";
    public static final String REQUEST_LATENCY = "request.latency";
    public static final String REQUEST_RECEIVED = "request.received";
    public static final String RESPONSE_SENT = "response.sent";
    public static final String RESPONSE_STATUS = "response.status";

    public static final String STATUS_CLASS_UNRECOGNISED = "unrecognised";


    private final MeterRegistry registry;
    private final String prefix;

    private final AtomicLong outstandingRequests;
    private final Timer latencyTimer;
    private final Counter requestsIncoming;

    private final Counter responsesSent;

    private final ConcurrentHashMap<Object, Long> ongoingRequests = new ConcurrentHashMap<>();

    private final NanoClock nanoClock;

    /**
     * Constructs a collector with a {@link MeterRegistry} to report stastistics to.
     *
     * @param registry a registry to report to
     * @param prefix a name prefix for all metrics generated by this collector
     */
    public RequestStatsCollector(MeterRegistry registry, String prefix) {
        this(registry, prefix, NanoClock.SYSTEM_CLOCK);
    }

    RequestStatsCollector(MeterRegistry registry, String prefix, NanoClock nanoClock) {

        this.registry = registry;
        this.prefix = prefix;

        this.nanoClock = nanoClock;
        this.outstandingRequests = registry.gauge(name(prefix, REQUEST_OUTSTANDING), new AtomicLong());
        this.latencyTimer = registry.timer(name(prefix, REQUEST_LATENCY));
        this.requestsIncoming = registry.counter(name(prefix, REQUEST_RECEIVED));
        this.responsesSent = registry.counter(name(prefix, RESPONSE_SENT));
    }

    @Override
    public void onRequest(Object requestId) {
        Long previous = this.ongoingRequests.putIfAbsent(requestId, nanoClock.nanoTime());
        if (previous == null) {
            this.outstandingRequests.incrementAndGet();
            this.requestsIncoming.increment();
        }
    }

    @Override
    public void onComplete(Object requestId, int responseStatus) {
        Long startTime = this.ongoingRequests.remove(requestId);
        if (startTime != null) {
            updateResponseStatusCounter(responseStatus);
            this.responsesSent.increment();
            this.outstandingRequests.decrementAndGet();

            this.latencyTimer.record(nanoClock.nanoTime() - startTime, NANOSECONDS);
        }
    }

    @Override
    public void onTerminate(Object requestId) {
        Long startTime = this.ongoingRequests.remove(requestId);
        if (startTime != null) {
            this.outstandingRequests.decrementAndGet();

            this.latencyTimer.record(nanoClock.nanoTime() - startTime, NANOSECONDS);
        }
    }

    private void updateResponseStatusCounter(int code) {
        String statusCodeClass = httpStatusCodeClass(code);
        Tags statusTags = Tags.of(STATUS_CLASS_TAG, statusCodeClass);

        statusTags = statusTags.and(STATUS_TAG, "5xx".equals(statusCodeClass)
                ? valueOf(code)
                : "");

        registry.counter(name(prefix, RESPONSE_STATUS), statusTags).increment();
    }

    private static String httpStatusCodeClass(int code) {
        if (code < 100 || code >= 600) {
            return STATUS_CLASS_UNRECOGNISED;
        }

        return code / 100 + "xx";
    }

    interface NanoClock {
        long nanoTime();

        NanoClock SYSTEM_CLOCK = System::nanoTime;
    }
}
