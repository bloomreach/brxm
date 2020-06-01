/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.service.IconSize;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IconTest extends WicketTester {

    @Test
    public void sprite_contains_all_icons() {
        final String sprite = Icon.getIconSprite();
        for (Icon icon : Icon.values()) {
            for (IconSize size : IconSize.values()) {
                final Pattern iconId = Pattern.compile("<symbol[^>]+id=\"" + icon.getSpriteId(size) + "\">");
                assertTrue("Sprite does not contain icon '" + icon + "' in size '" + size + "'", iconId.matcher(sprite).find());
            }
        }
    }

    @Test
    public void css_classes_are_set() {
        assertEquals("hi hi-floppy hi-m", Icon.FLOPPY.getCssClasses(IconSize.M));
        assertEquals("hi hi-arrow-up hi-xl", Icon.ARROW_UP.getCssClasses(IconSize.XL));
    }

    @Test
    public void sprite_reference_is_rendered() {
        assertEquals("<svg class=\"hi hi-bell hi-m\"><use xlink:href=\"#hi-bell-m\" /></svg>", Icon.BELL.getSpriteReference(IconSize.M));
        assertEquals("<svg class=\"hi hi-bell hi-m\"><use xlink:href=\"#hi-bell-m\" /></svg>", Icon.BELL.getSpriteReference(IconSize.M, null));
        assertEquals("<svg class=\"hi hi-bell hi-m\"><use xlink:href=\"#hi-bell-m\" /></svg>", Icon.BELL.getSpriteReference(IconSize.M, ""));
        assertEquals("<svg class=\"hi hi-bell hi-m\"><use xlink:href=\"#hi-bell-m\" /></svg>", Icon.BELL.getSpriteReference(IconSize.M, " "));
        assertEquals("<svg class=\"hi hi-bell hi-m foo\"><use xlink:href=\"#hi-bell-m\" /></svg>", Icon.BELL.getSpriteReference(IconSize.M, "foo"));
        assertEquals("<svg class=\"hi hi-bell hi-m foo bar\"><use xlink:href=\"#hi-bell-m\" /></svg>", Icon.BELL.getSpriteReference(IconSize.M, "foo", "bar"));
    }

}
