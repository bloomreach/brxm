/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.channel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.core.parameters.Parameter;

public class ChannelInfoClassProcessor {

    public static List<HstPropertyDefinition> getProperties(Class<? extends ChannelInfo> channelInfoClass) {
        List<HstPropertyDefinition> properties = new ArrayList<HstPropertyDefinition>();
        for (Method method : channelInfoClass.getMethods()) {
            if (method.isAnnotationPresent(Parameter.class)) {
                // new style annotations
                Parameter propAnnotation = method.getAnnotation(Parameter.class);
                if (propAnnotation.hideInChannelManager()) {
                    continue;
                }
                HstPropertyDefinition prop = new AnnotationHstPropertyDefinition(propAnnotation, method.getReturnType(), method.getAnnotations());
                properties.add(prop);
            }
        }
        return properties;
    }
}
