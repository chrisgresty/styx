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
package com.hotels.styx.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public class MutableHttpHeaders extends HttpHeaders {

    public MutableHttpHeaders() {
        super();
    }

    public MutableHttpHeaders(HttpHeaders headers) {
        super();
        nettyHeaders.set(headers.nettyHeaders);
    }

    /**
     * Adds a new header with the specified {@code name} and {@code value}.
     * <p/>
     * Will not replace any existing values for the header.
     *
     * @param name  The name of the header
     * @param value The value of the header
     * @return this builder
     */
    public MutableHttpHeaders add(CharSequence name, String value) {
        this.nettyHeaders.add(name, requireNonNull(value));
        return this;
    }

    /**
     * Adds a new header with the specified {@code name} and {@code value}.
     * <p/>
     * Will not replace any existing values for the header.
     *
     * @param name  The name of the header
     * @param value The value of the header
     * @return this builder
     */
    public MutableHttpHeaders add(CharSequence name, Object value) {
        this.nettyHeaders.add(name, requireNonNull(value));
        return this;
    }

    /**
     * Adds a new header with the specified {@code name} and {@code values}.
     * <p/>
     * Will not replace any existing values for the header.
     *
     * @param name   The name of the header
     * @param values The value of the header
     * @return this builder
     */
    public MutableHttpHeaders add(CharSequence name, Iterable values) {
        nonNullValues(values)
                .ifPresent(nonNullValues -> this.nettyHeaders.add(name, nonNullValues));

        return this;
    }

    private Optional<List<?>> nonNullValues(Iterable<?> values) {
        List<?> list = stream(values.spliterator(), false)
                .filter(value -> value != null)
                .collect(toList());

        return list.isEmpty() ? Optional.empty() : Optional.of(list);
    }

    /**
     * Removes the header with the specified {@code name}.
     *
     * @param name the name of the header to remove
     * @return this builder
     */
    public MutableHttpHeaders remove(CharSequence name) {
        this.nettyHeaders.remove(name);
        return this;
    }

    /**
     * Sets the (only) value for the header with the specified name.
     * <p/>
     * All existing values for the same header will be removed.
     *
     * @param name  The name of the header
     * @param value The value of the header
     * @return this builder
     */
    public MutableHttpHeaders set(CharSequence name, String value) {
        nettyHeaders.set(name, value);
        return this;
    }

    /**
     * Sets the (only) value for the header with the specified name.
     * <p/>
     * All existing values for the same header will be removed.
     *
     * @param name  The name of the header
     * @param value The value of the header
     * @return this builder
     */
    public MutableHttpHeaders set(CharSequence name, Instant value) {
        nettyHeaders.set(name, RFC1123_DATE_FORMAT.format(value));
        return this;
    }

    /**
     * Sets the (only) value for the header with the specified name.
     * <p/>
     * All existing values for the same header will be removed.
     *
     * @param name  The name of the header
     * @param value The value of the header
     * @return this builder
     */
    public MutableHttpHeaders set(CharSequence name, Object value) {
        nettyHeaders.set(name, value);
        return this;
    }

    /**
     * Sets the (only) value for the header with the specified name.
     * <p/>
     * All existing values for the same header will be removed.
     *
     * @param name   The name of the header
     * @param values The value of the header
     * @return this builder
     */
    public MutableHttpHeaders set(CharSequence name, Iterable values) {
        nonNullValues(values)
                .ifPresent(nonNullValues -> this.nettyHeaders.set(name, nonNullValues));

        return this;
    }

    /**
     * Sets the (only) value for the header with the specified name.
     * <p/>
     * All existing values for the same header will be removed.
     *
     * @param name  The name of the header
     * @param value The value of the header
     * @return this builder
     */
    public MutableHttpHeaders set(CharSequence name, int value) {
        nettyHeaders.set(name, value);
        return this;
    }


}
