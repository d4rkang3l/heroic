/*
 * Copyright (c) 2015 Spotify AB.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.heroic.filter;

import com.spotify.heroic.common.Series;
import com.spotify.heroic.grammar.QueryParser;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = {"OPERATOR", "tag", "value"}, doNotUseGetters = true)
public class MatchTagFilter implements Filter {
    public static final String OPERATOR = "=";

    private final String tag;
    private final String value;

    @Override
    public boolean apply(Series series) {
        final String value;
        return (value = series.getTags().get(tag)) != null && value.equals(this.value);
    }

    @Override
    public <T> T visit(final Visitor<T> visitor) {
        return visitor.visitMatchTag(this);
    }

    @Override
    public String toString() {
        return "[" + OPERATOR + ", " + tag + ", " + value + "]";
    }

    @Override
    public MatchTagFilter optimize() {
        return this;
    }

    @Override
    public String operator() {
        return OPERATOR;
    }

    @Override
    public int compareTo(Filter o) {
        if (!MatchTagFilter.class.isAssignableFrom(o.getClass())) {
            return operator().compareTo(o.operator());
        }

        final MatchTagFilter other = (MatchTagFilter) o;
        final int first = tag.compareTo(other.tag);

        if (first != 0) {
            return first;
        }

        return value.compareTo(other.value);
    }

    @Override
    public String toDSL() {
        return QueryParser.escapeString(tag) + " = " + QueryParser.escapeString(value);
    }
}
