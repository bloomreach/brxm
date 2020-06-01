/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.editor.validator;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.ModelPathElement;

/**
 * Filter validation results.  Useful to pass the results of a validation to
 * plugins that are unaware of the container model, but only operate on a field
 * value.
 */
public class FilteredValidationModel extends Model<IValidationResult> {

    private static final long serialVersionUID = 1L;

    public FilteredValidationModel(IModel<IValidationResult> upstreamModel, ModelPathElement element) {
        super(new FilteredValidationResult(upstreamModel.getObject(), element));
    }

}
