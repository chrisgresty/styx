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
package com.hotels.styx.server.netty.connectors;

import com.hotels.styx.api.HttpResponseStatus;
import com.hotels.styx.common.Pair;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.hotels.styx.common.Collections.copyToUnmodifiableMap;
import static com.hotels.styx.common.Pair.pair;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Default list of exception mappers.
 */
final class ExceptionStatusMapper {
    private static final Logger LOG = getLogger(ExceptionStatusMapper.class);

    private final Map<HttpResponseStatus, List<Class<? extends Exception>>> multimap;

    private ExceptionStatusMapper(Builder builder) {
        this.multimap = copyToUnmodifiableMap(builder.multimap);
    }

    Optional<HttpResponseStatus> statusFor(Throwable throwable) {
        List<HttpResponseStatus> matchingStatuses = this.multimap.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(clazz -> pair(entry.getKey(), clazz)))
                .filter(pair -> pair.value().isInstance(throwable))
                .sorted(comparing(pair -> pair.key().code()))
                .map(Pair::key)
                .collect(toList());

        if (matchingStatuses.size() > 1) {
            LOG.error("Multiple matching statuses for throwable={} statuses={}", throwable, matchingStatuses);
            return Optional.empty();
        }

        return matchingStatuses.stream().findFirst();
    }

    static final class Builder {
        private final Map<HttpResponseStatus, List<Class<? extends Exception>>> multimap;

        public Builder() {
            this.multimap = new HashMap<>();
        }

        @SafeVarargs
        public final Builder add(HttpResponseStatus status, Class<? extends Exception>... classes) {
            if (multimap.containsKey(status)) {
                multimap.get(status).addAll(asList(classes));
            } else {
                multimap.put(status, asList(classes));
            }
            return this;
        }

        public ExceptionStatusMapper build() {
            return new ExceptionStatusMapper(this);
        }
    }
}
