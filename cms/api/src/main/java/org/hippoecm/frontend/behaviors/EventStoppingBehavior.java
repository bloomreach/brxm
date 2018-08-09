/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.behaviors;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventStoppingBehavior extends Behavior {
    public static final Logger log = LoggerFactory.getLogger(EventStoppingBehavior.class);

    private final String event;

    public EventStoppingBehavior(final String event) {
        this.event = checkEvent(event);
    }

    private static String checkEvent(final String event) {
        if (event.startsWith("on"))
        {
            final String shortName = event.substring(2);
            // TODO Wicket 8 Change this to throw an error in the milestone/RC versions and remove it for the final version
            log.warn("Since version 6.0.0 Wicket uses JavaScript event registration so there is no need " +
                     "of the leading 'on' in the event name '{}'. Please use just '{}'."
                    , event, shortName);
            return shortName;
        }
        return event;
    }

    @Override
    public void onComponentTag(final Component component, final ComponentTag tag) {
        super.onComponentTag(component, tag);

        // We don't use jQuery event registration hence we prepend the event with "on"
        tag.put("on" + event, "Wicket.Event.stop(event);");
    }

}
