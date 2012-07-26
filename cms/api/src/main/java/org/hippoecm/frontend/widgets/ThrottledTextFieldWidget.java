/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.widgets;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Duration;

public class ThrottledTextFieldWidget extends TextFieldWidget {

    public static final Duration THROTTLE_DELAY = Duration.milliseconds(750);

    public ThrottledTextFieldWidget(String id, IModel<String> model) {
        super(id, model, null, THROTTLE_DELAY);
    }

    public ThrottledTextFieldWidget(String id, IModel<String> model, Duration throttleDelay) {
        super(id, model, null, throttleDelay);
    }

    public ThrottledTextFieldWidget(String id, IModel<String> model, IModel<String> labelModel) {
        super(id, model, labelModel, THROTTLE_DELAY);
    }

    public ThrottledTextFieldWidget(String id, IModel<String> model, IModel<String> labelModel, Duration throttleDelay) {
        super(id, model, labelModel, throttleDelay);
    }
}
