/*
 * Copyright 2019-2022 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.frontend.wicketevents;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.tester.WicketTestCase;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(EasyMockRunner.class)
public class AjaxCallFailureHeaderItemTest extends WicketTestCase {

    @SuppressWarnings("unused")
    @Mock
    private Response response;

    @Before
    public void beForeEach() {
        // We need to manually call the before methods that are annotated with JUnit5
        super.commonBefore();
    }

    @Test
    public void test_script_subscribes_to_ajax_call_failure_events() {

        final Capture<String> data = Capture.newInstance(CaptureType.ALL);
        response.write(capture(data));
        expectLastCall().atLeastOnce();
        replay(response);

        final HeaderItem headerItem = new AjaxCallFailureHeaderItem();
        headerItem.render(response);

        final String script = String.join("\n", data.getValues());
        assertThat(script, allOf(
                containsString("Wicket.Event.subscribe"),
                containsString("Wicket.Event.Topic.AJAX_CALL_FAILURE")
        ));

        verify(response);
    }
}
