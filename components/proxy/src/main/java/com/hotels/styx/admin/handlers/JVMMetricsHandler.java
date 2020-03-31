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
package com.hotels.styx.admin.handlers;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;
import com.codahale.metrics.json.MetricsModule;
import com.hotels.styx.api.MetricRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

/**
 * Handler for showing the JVM statistics. Can cache page content.
 */
public class JVMMetricsHandler extends JsonHandler<MetricRegistry> {
    private static final Predicate<String> STARTS_WITH_JVM = input -> input.startsWith("jvm");
    private static final boolean DO_NOT_SHOW_SAMPLES = false;

    /**
     * Constructs a new handler.
     *
     * @param metricRegistry  metrics registry
     * @param cacheExpiration duration for which generated page content should be cached
     */
    public JVMMetricsHandler(MetricRegistry metricRegistry, Optional<Duration> cacheExpiration) {
        super(new FilteredRegistry(metricRegistry), cacheExpiration, new MetricsModule(SECONDS, MILLISECONDS, DO_NOT_SHOW_SAMPLES));
    }

    private static final class FilteredRegistry implements MetricRegistry {
        private final MetricRegistry original;

        public FilteredRegistry(MetricRegistry original) {
            this.original = original;
        }

        @Override
        public MetricRegistry scope(String name) {
            return null;
        }

        @Override
        public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
            return null;
        }

        @Override
        public boolean deregister(String name) {
            return false;
        }

        @Override
        public Counter counter(String name) {
            return null;
        }

        @Override
        public Histogram histogram(String name) {
            return null;
        }

        @Override
        public Meter meter(String name) {
            return null;
        }

        @Override
        public Timer timer(String name) {
            return null;
        }

        @Override
        public void addListener(MetricRegistryListener listener) {
        }

        @Override
        public void removeListener(MetricRegistryListener listener) {
        }

        @Override
        public SortedSet<String> getNames() {
            return new FilteredSortedSet<String>(original.getNames(), STARTS_WITH_JVM);
        }

        @Override
        public SortedMap<String, Gauge> getGauges() {
            return new FilteredSortedMap<>(original.getGauges(), STARTS_WITH_JVM);
        }

        @Override
        public SortedMap<String, Gauge> getGauges(MetricFilter filter) {
            return null;
        }

        @Override
        public SortedMap<String, Counter> getCounters() {
            return new FilteredSortedMap<>(original.getCounters(), STARTS_WITH_JVM);
        }

        @Override
        public SortedMap<String, Counter> getCounters(MetricFilter filter) {
            return null;
        }

        @Override
        public SortedMap<String, Histogram> getHistograms() {
            return new FilteredSortedMap<>(original.getHistograms(), STARTS_WITH_JVM);
        }

        @Override
        public SortedMap<String, Histogram> getHistograms(MetricFilter filter) {
            return null;
        }

        @Override
        public SortedMap<String, Meter> getMeters() {
            return new FilteredSortedMap<>(original.getMeters(), STARTS_WITH_JVM);
        }

        @Override
        public SortedMap<String, Meter> getMeters(MetricFilter filter) {
            return null;
        }

        @Override
        public SortedMap<String, Timer> getTimers() {
            return new FilteredSortedMap<>(original.getTimers(), STARTS_WITH_JVM);
        }

        @Override
        public SortedMap<String, Timer> getTimers(MetricFilter filter) {
            return null;
        }

        @Override
        public SortedMap<String, Metric> getMetrics() {
            return new FilteredSortedMap<>(original.getMetrics(), STARTS_WITH_JVM);
        }
    }

    static class FilteredSortedMap<K, V> implements SortedMap<K, V> {

        private final SortedMap<K, V> unfiltered;
        private final Predicate<K> keyFilter;

        public FilteredSortedMap(SortedMap<K, V> unfiltered, Predicate<K> keyFilter) {
            this.unfiltered = unfiltered;
            this.keyFilter = keyFilter;
        }

        @Override
        public Comparator<? super K> comparator() {
            return unfiltered.comparator();
        }

        @NotNull
        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return new FilteredSortedMap<>(unfiltered.subMap(fromKey, toKey), keyFilter);
        }

        @NotNull
        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return new FilteredSortedMap<>(unfiltered.headMap(toKey), keyFilter);
        }

        @NotNull
        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return new FilteredSortedMap<>(unfiltered.tailMap(fromKey), keyFilter);
        }

        @Override
        public K firstKey() {
            return unfiltered.keySet().stream().filter(keyFilter).findFirst().orElseThrow(NoSuchElementException::new);
        }

        @Override
        public K lastKey() {
            return unfiltered.keySet().stream().filter(keyFilter).reduce((a, b) -> b).orElseThrow(NoSuchElementException::new);
        }

        @Override
        public int size() {
            return (int) unfiltered.keySet().stream().filter(keyFilter).count();
        }

        @Override
        public boolean isEmpty() {
            return unfiltered.keySet().stream().noneMatch(keyFilter);
        }

        @Override
        public boolean containsKey(Object key) {
            return unfiltered.containsKey(key) && keyFilter.test((K) key);
        }

        @Override
        public boolean containsValue(Object value) {
            return unfiltered.entrySet().stream()
                    .anyMatch(e -> Objects.equals(e.getValue(), value) && keyFilter.test(e.getKey()));
        }

        @Override
        public V get(Object key) {
            V value = unfiltered.get(key);
            return value != null && keyFilter.test((K) key) ? value : null;
        }

        @Nullable
        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(@NotNull Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<K> keySet() {
            return new FilteredSet<K>(unfiltered.keySet(), keyFilter);
        }

        @Override
        public Collection<V> values() {
            return new AbstractCollection<V>() {

                private Set<Entry<K, V>> entries = entrySet();

                @Override
                public Iterator<V> iterator() {
                    return new Iterator<V>() {

                        Iterator<Entry<K, V>> entryIterator = entries.iterator();

                        @Override
                        public boolean hasNext() {
                            return entryIterator.hasNext();
                        }

                        @Override
                        public V next() {
                            return entryIterator.next().getValue();
                        }
                    };
                }

                @Override
                public int size() {
                    return entries.size();
                }
            };
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new FilteredSet<Entry<K, V>>(unfiltered.entrySet(), e -> keyFilter.test(e.getKey()));
        }
    }

    static class FilteredSet<K> implements Set<K> {

        final Set<K> unfiltered;
        final Predicate<K> filter;

        public FilteredSet(Set<K> unfiltered, Predicate<K> filter) {
            this.unfiltered = unfiltered;
            this.filter = filter;
        }

        @Override
        public int size() {
            return (int) unfiltered.stream().filter(filter).count();
        }

        @Override
        public boolean isEmpty() {
            return unfiltered.stream().noneMatch(filter);
        }

        @Override
        public boolean contains(Object o) {
            return unfiltered.contains(o) && filter.test((K) o);
        }

        @NotNull
        @Override
        public Iterator<K> iterator() {
            return unfiltered.stream().filter(filter).collect(toList()).iterator();
        }

        @NotNull
        @Override
        public Object[] toArray() {
            return unfiltered.stream().filter(filter).toArray();
        }

        @NotNull
        @Override
        public <T> T[] toArray(@NotNull T[] a) {
            int size = size();
            T[] array = a.length <= size
                    ? a
                    : (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
            return unfiltered.stream().filter(filter).toArray(n -> array);
        }

        @Override
        public boolean add(K string) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            return c.stream().allMatch(this::contains);
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends K> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    static class FilteredSortedSet<K> extends FilteredSet<K> implements SortedSet<K> {

        FilteredSortedSet(SortedSet<K> unfiltered, Predicate<K> filter) {
            super(unfiltered, filter);
        }

        private SortedSet<K> unfiltered() {
            return (SortedSet<K>) unfiltered;
        }

        @Nullable
        @Override
        public Comparator<? super K> comparator() {
            return unfiltered().comparator();
        }

        @NotNull
        @Override
        public SortedSet<K> subSet(K fromElement, K toElement) {
            return new FilteredSortedSet<>(unfiltered().subSet(fromElement, toElement), filter);
        }

        @NotNull
        @Override
        public SortedSet<K> headSet(K toElement) {
            return new FilteredSortedSet<>(unfiltered().headSet(toElement), filter);
        }

        @NotNull
        @Override
        public SortedSet<K> tailSet(K fromElement) {
            return new FilteredSortedSet<>(unfiltered().tailSet(fromElement), filter);
        }

        @Override
        public K first() {
            return unfiltered().stream().filter(filter).findFirst().orElseThrow(NoSuchElementException::new);
        }

        @Override
        public K last() {
            return unfiltered().stream().filter(filter).reduce((a, b) -> b).orElseThrow(NoSuchElementException::new);
        }
    }
}
