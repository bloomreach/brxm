package org.hippoecm.repository.util;

import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DateToolsTest {

    @Test
    public void testGetSupportedDateResolutionsReturnsNewArrayInstance() {
        final DateTools.Resolution[] r1 = DateTools.getSupportedDateResolutions();
        final DateTools.Resolution[] r2 = DateTools.getSupportedDateResolutions();
        // r1 and r2 contain the same elements in the same order
        assertThat(asList(r1), is(asList(r2)));
        // but they are different instances
        assertThat(r1.equals(r2), is(false));
        assertThat(r2.equals(r1), is(false));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetSupportedResolutionRemoveUnsupported() {
        final List<DateTools.Resolution> resolutions = (List<DateTools.Resolution>)DateTools.getSupportedResolutions();
        resolutions.remove(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetSupportedResolutionsSetUnsupported() {
        final List<DateTools.Resolution> resolutions = (List<DateTools.Resolution>)DateTools.getSupportedResolutions();
        resolutions.set(0, DateTools.Resolution.MILLISECOND);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetSupportedResolutionsAddUnsupported() {
        final List<DateTools.Resolution> resolutions = (List<DateTools.Resolution>)DateTools.getSupportedResolutions();
        resolutions.add(resolutions.size(), DateTools.Resolution.MILLISECOND);
    }
}
