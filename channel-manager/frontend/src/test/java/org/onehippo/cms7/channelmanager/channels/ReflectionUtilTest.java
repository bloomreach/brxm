package org.onehippo.cms7.channelmanager.channels;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests {@link ReflectionUtil}.
 */
public class ReflectionUtilTest {

    @Test
    public void stringValue() throws URISyntaxException {
        assertEquals("www.example.com", ReflectionUtil.getStringValue(new URI("http://www.example.com"), "host"));
        assertEquals("true", ReflectionUtil.getStringValue(new URI("http://www.example.com"), "absolute"));
    }

    @Test
    public void nonExistingGetters() throws URISyntaxException {
        assertNull(ReflectionUtil.getStringValue(new URI("http://www.example.com"), "nosuchgetter"));
    }

    @Test
    public void gettersDoNotHaveArguments() {
        assertNull(ReflectionUtil.getStringValue(Calendar.getInstance(), "greatestMinimum"));
        assertNull(ReflectionUtil.getStringValue(Calendar.getInstance(), "set"));
    }

    @Test
    public void nullValueReturnsEmptyString() throws URISyntaxException {
        assertEquals("", ReflectionUtil.getStringValue(new URI("/foo/bar"), "host"));
    }

}
