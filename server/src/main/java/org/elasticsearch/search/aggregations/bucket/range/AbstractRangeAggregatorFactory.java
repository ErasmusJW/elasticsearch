/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.search.aggregations.bucket.range;

import org.elasticsearch.search.aggregations.Aggregator;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.aggregations.AggregatorFactory;
import org.elasticsearch.search.aggregations.CardinalityUpperBound;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregator.Range;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregator.Unmapped;
import org.elasticsearch.search.aggregations.support.AggregationContext;
import org.elasticsearch.search.aggregations.support.CoreValuesSourceType;
import org.elasticsearch.search.aggregations.support.ValuesSource.Numeric;
import org.elasticsearch.search.aggregations.support.ValuesSourceAggregatorFactory;
import org.elasticsearch.search.aggregations.support.ValuesSourceConfig;
import org.elasticsearch.search.aggregations.support.ValuesSourceRegistry;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AbstractRangeAggregatorFactory<R extends Range> extends ValuesSourceAggregatorFactory {

    private final InternalRange.Factory<?, ?> rangeFactory;
    private final R[] ranges;
    private final boolean keyed;
    private final ValuesSourceRegistry.RegistryKey<RangeAggregatorSupplier> registryKey;

    public static void registerAggregators(
        ValuesSourceRegistry.Builder builder,
        ValuesSourceRegistry.RegistryKey<RangeAggregatorSupplier> registryKey
    ) {
        builder.register(
            registryKey,
            List.of(CoreValuesSourceType.NUMERIC, CoreValuesSourceType.DATE, CoreValuesSourceType.BOOLEAN),
            RangeAggregator::new,
                true);
    }

    public AbstractRangeAggregatorFactory(String name,
                                          ValuesSourceRegistry.RegistryKey<RangeAggregatorSupplier> registryKey,
                                          ValuesSourceConfig config,
                                          R[] ranges,
                                          boolean keyed,
                                          InternalRange.Factory<?, ?> rangeFactory,
                                          AggregationContext context,
                                          AggregatorFactory parent,
                                          AggregatorFactories.Builder subFactoriesBuilder,
                                          Map<String, Object> metadata) throws IOException {
        super(name, config, context, parent, subFactoriesBuilder, metadata);
        this.ranges = ranges;
        this.keyed = keyed;
        this.rangeFactory = rangeFactory;
        this.registryKey = registryKey;
    }

    @Override
    protected Aggregator createUnmapped(SearchContext searchContext,
                                            Aggregator parent,
                                            Map<String, Object> metadata) throws IOException {
        return new Unmapped<>(name, ranges, keyed, config.format(), searchContext, parent, rangeFactory, metadata);
    }

    @Override
    protected Aggregator doCreateInternal(
        SearchContext searchContext,
        Aggregator parent,
        CardinalityUpperBound cardinality,
        Map<String, Object> metadata
    ) throws IOException {

        return context.getValuesSourceRegistry()
            .getAggregator(registryKey, config)
            .build(
                name,
                factories,
                (Numeric) config.getValuesSource(),
                config.format(),
                rangeFactory,
                ranges,
                keyed,
                searchContext,
                parent,
                cardinality,
                metadata
            );
    }
}
