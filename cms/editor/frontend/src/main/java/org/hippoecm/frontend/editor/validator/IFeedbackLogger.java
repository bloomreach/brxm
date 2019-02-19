/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.validation.FeedbackScope;

public interface IFeedbackLogger {

    /**
     * Log a scoped Feedback warning.
     *
     * @param message description of the warning
     * @param scope   the level this warning applies to
     */
    void warn(IModel<String> message, FeedbackScope scope);

    /**
     * Log a scoped Feedback error.
     *
     * @param message description of the error
     * @param scope   the level this error applies to
     */
    void error(IModel<String> message, FeedbackScope scope);

}
