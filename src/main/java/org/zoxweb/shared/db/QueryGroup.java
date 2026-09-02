/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.db;

import org.zoxweb.shared.util.GetValue;

/**
 * Explicit grouping (parentheses) markers for query criteria. Without grouping, a marker
 * sequence renders strictly left to right and the target language's operator precedence
 * decides the meaning — in SQL, {@code a AND b OR c} silently parses as {@code (a AND b) OR c}.
 * These markers let the caller state the intended structure:
 * <pre>
 *   // status = 'active' AND (type = 'a' OR type = 'b')
 *   search(nvce, null,
 *       new QueryMatchString("status", "active", RelationalOperator.EQUAL),
 *       Const.LogicalOperator.AND,
 *       QueryGroup.OPEN,
 *           new QueryMatchString("type", "a", RelationalOperator.EQUAL),
 *           Const.LogicalOperator.OR,
 *           new QueryMatchString("type", "b", RelationalOperator.EQUAL),
 *       QueryGroup.CLOSE);
 * </pre>
 * Groups may nest. Formatters must reject an unbalanced sequence.
 */
public enum QueryGroup
        implements GetValue<String>, QueryMarker {

    OPEN("("),
    CLOSE(")"),
    ;

    private final String value;

    QueryGroup(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
