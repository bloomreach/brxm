/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.skin;

import java.util.regex.Pattern;

import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.tester.WicketTesterHelper;
import org.apache.wicket.util.tester.WicketTesterScope;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class IconTest extends WicketTester {

    @Test
    public void sprite_contains_all_icons() {
        final String sprite = Icon.getIconSprite();
        for (Icon icon : Icon.values()) {
            final Pattern iconId = Pattern.compile("<symbol[^>]+id=\"" + icon.getSpriteId() + "\">");
            assertTrue("Sprite should contain icon '" + icon + "'", iconId.matcher(sprite).find());
        }
    }

}
