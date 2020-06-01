/*
 *  Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Application;
import org.apache.wicket.markup.html.IPackageResourceGuard;
import org.apache.wicket.markup.html.SecurePackageResourceGuard;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.settings.ResourceSettings;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

public class IconUtilTest extends WicketTester {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @Before
    public void setUp() {
        final ResourceSettings resourceSettings = Application.get().getResourceSettings();
        final IPackageResourceGuard packageResourceGuard = resourceSettings.getPackageResourceGuard();
        if (packageResourceGuard instanceof SecurePackageResourceGuard) {
            SecurePackageResourceGuard guard = (SecurePackageResourceGuard) packageResourceGuard;
            guard.setAllowAccessToRootResources(true);
        }
    }

    @Test(expected = ResourceStreamNotFoundException.class)
    public void non_existing_svg_file() throws Exception {
        IconUtil.svgAsString(new PackageResourceReference("/test-SVG-non-existing.svg"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void broken_svg_file() throws Exception {
        IconUtil.svgAsString(new PackageResourceReference("/test-SVG-invalid.svg"));
    }

    @Test
    public void svg_as_string_from_resource_reference() throws Exception {
        String svgAsString = IconUtil.svgAsString(new PackageResourceReference("/test-SVG.svg"));
        assertThat(svgAsString, startsWith("<svg"));
        assertThat(svgAsString, endsWith("</svg>" + LINE_SEPARATOR));

        svgAsString = IconUtil.svgAsString(new PackageResourceReference("/test-SVG-with-simple-root-element.svg"));
        assertThat(svgAsString, startsWith("<svg"));
        assertThat(svgAsString, endsWith("</svg>" + LINE_SEPARATOR));

        svgAsString = IconUtil.svgAsString(new PackageResourceReference("/test-SVG-with-newline-after-root-element.svg"));
        assertThat(svgAsString, startsWith("<svg"));
        assertThat(svgAsString, endsWith("</svg>" + LINE_SEPARATOR));
    }

    @Test
    public void svg_as_string_with_empty_classes() throws Exception {
        String svgAsString = IconUtil.svgAsString(new PackageResourceReference("/test-SVG.svg"), null);
        assertThat(svgAsString, not(containsString("class=\"")));

        svgAsString = IconUtil.svgAsString(new PackageResourceReference("/test-SVG.svg"), "");
        assertThat(svgAsString, not(containsString("class=\"")));

        svgAsString = IconUtil.svgAsString(new PackageResourceReference("/test-SVG.svg"), " ");
        assertThat(svgAsString, not(containsString("class=\"")));
    }

    @Test
    public void svg_as_string_with_css_classes() throws Exception {
        String svgAsString = IconUtil.svgAsString(new PackageResourceReference("/test-SVG.svg"), "classA");
        assertThat(svgAsString, startsWith("<svg class=\"classA\""));

        svgAsString = IconUtil.svgAsString(new PackageResourceReference("/test-SVG.svg"), "classA", "classB");
        assertThat(svgAsString, startsWith("<svg class=\"classA classB\""));

        svgAsString = IconUtil.svgAsString(new PackageResourceReference("/test-SVG.svg"), "classA classB");
        assertThat(svgAsString, startsWith("<svg class=\"classA classB\""));

        svgAsString = IconUtil.svgAsString(new PackageResourceReference("/test-SVG-with-class-attribute.svg"));
        assertThat(svgAsString, startsWith("<svg version=\"1.0\" class=\"classY classZ\""));

        svgAsString = IconUtil.svgAsString(new PackageResourceReference("/test-SVG-with-class-attribute.svg"), "classA");
        assertThat(svgAsString, startsWith("<svg version=\"1.0\" class=\"classA classY classZ\""));

        svgAsString = IconUtil.svgAsString(new PackageResourceReference("/test-SVG-with-class-attribute.svg"), "classA", "classB");
        assertThat(svgAsString, startsWith("<svg version=\"1.0\" class=\"classA classB classY classZ\""));

        svgAsString = IconUtil.svgAsString(new PackageResourceReference("/test-SVG-with-class-attribute-in-descending-elements.svg"), "classA");
        assertThat(svgAsString, startsWith("<svg class=\"classA\""));
        assertThat(svgAsString, containsString("class=\"classZ\""));
    }
}
