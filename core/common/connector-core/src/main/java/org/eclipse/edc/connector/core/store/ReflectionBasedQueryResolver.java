/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.core.store;

import org.eclipse.edc.spi.query.CriterionToPredicateConverter;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.util.reflection.ReflectionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Default implementation of {@link QueryResolver} that applies query on a stream. Uses reflection to fetch object fields. Used in stores implementations.
 *
 * @param <T> type of the stream elements.
 */
public class ReflectionBasedQueryResolver<T> implements QueryResolver<T> {

    private final Class<T> typeParameterClass;
    private final CriterionToPredicateConverter predicateConverter = new CriterionToPredicateConverterImpl();

    /**
     * Constructor for StreamQueryResolver
     *
     * @param typeParameterClass class of the type parameter. Used in reflection operation to recursively fetch a property from an object.
     */
    public ReflectionBasedQueryResolver(Class<T> typeParameterClass) {
        this.typeParameterClass = typeParameterClass;
    }

    /**
     * Method to query a stream by provided specification.
     * Converts the criterion into 'and' predicate.
     * Applies sorting. When sort field is not found returns empty stream.
     * Applies offset and limit on the query result.
     *
     * @param stream stream to be queried.
     * @param spec query specification.
     * @return stream result from queries.
     */
    @Override
    public Stream<T> query(Stream<T> stream, QuerySpec spec) {
        var andPredicate = spec.getFilterExpression().stream()
                .map(predicateConverter::convert)
                .reduce(x -> true, Predicate::and);

        var filteredStream  = stream.filter(andPredicate);

        // sort
        var sortField = spec.getSortField();

        if (sortField != null) {
            if (ReflectionUtil.getFieldRecursive(typeParameterClass, sortField) == null) {
                throw new IllegalArgumentException(format("Cannot sort by %s, the field does not exist in %s", sortField, typeParameterClass));
            }
            var comparator = propertyComparator(spec.getSortOrder() == SortOrder.ASC, sortField);
            filteredStream = filteredStream.sorted(comparator);
        }

        // limit
        return filteredStream.skip(spec.getOffset()).limit(spec.getLimit());
    }

    @NotNull
    private Comparator<T> propertyComparator(boolean isAscending, String property) {
        return (obj1, obj2) -> {
            var o1 = ReflectionUtil.getFieldValue(property, obj1);
            var o2 = ReflectionUtil.getFieldValue(property, obj2);

            if (o1 == null || o2 == null) {
                return 0;
            }

            if (!(o1 instanceof Comparable)) {
                throw new IllegalArgumentException("A property '" + property + "' is not comparable!");
            }
            var comp1 = (Comparable) o1;
            var comp2 = (Comparable) o2;
            return isAscending ? comp1.compareTo(comp2) : comp2.compareTo(comp1);
        };
    }

}
