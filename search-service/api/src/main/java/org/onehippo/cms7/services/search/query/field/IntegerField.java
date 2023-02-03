/*
 * Copyright 2012-2023 Bloomreach
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.search.query.field;

import org.onehippo.cms7.services.search.query.constraint.IntegerConstraint;
import org.onehippo.cms7.services.search.query.constraint.LowerBoundedIntegerConstraint;
import org.onehippo.cms7.services.search.query.constraint.UpperBoundedIntegerConstraint;

public final class IntegerField extends Field {

    public IntegerField(final String property) {
        super(property);
    }

    public IntegerConstraint isEqualTo(final int value) {
        return new IntegerConstraint(getProperty(), value, IntegerConstraint.Type.EQUAL);
    }

    public LowerBoundedIntegerConstraint from(final int value) {
        return new IntegerConstraint(getProperty(), value, IntegerConstraint.Type.FROM);
    }

    public UpperBoundedIntegerConstraint to(final int value) {
        return new IntegerConstraint(getProperty(), value, IntegerConstraint.Type.TO);
    }
}
