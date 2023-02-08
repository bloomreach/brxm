/*
 *  Copyright 2015-2023 Bloomreach
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
package org.hippoecm.frontend.skin;

import org.apache.wicket.Component;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.service.IconSize;

public interface IconProvider extends IClusterable {

    default Component getIcon(final String id, IconSize size) {
        return null;
    }

}
