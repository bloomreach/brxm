package org.hippoecm.hst.servlet;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.servlet.ServletConfig;

import org.apache.james.mime4j.codec.DecoderUtil;
import org.apache.james.mime4j.util.MimeUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests {@link org.hippoecm.hst.servlet.BinariesServlet}. Since the tested class is originally written by Hippo,
 * only the added logic is tested. The original logic is left untested.
 *
 * @author Tom van Zummeren
 */
public class BinariesServletTest {

    private BinariesServlet binariesServlet;

    /**
     * @see Before
     */
    @Before
    public void setUp() {
        binariesServlet = new BinariesServlet();
    }

    /**
     * Precondition: All possible init-params are set.
     *
     * Pass condition: Content types init-param is properly parsed as an array of Strings and the filename property is
     *                 properly retrieved from init-params.
     */
    @Test
    public void testInit() throws Exception {
        ServletConfig servletConfig = createMock(ServletConfig.class);

        String mimeTypesString = "\n" +
                "                application/pdf\n\r" +
                "                application/rtf\n" +
                "                application/excel\r\n" +
                "            ";
        String expectedFilenameProperty = "myschema:filename";
        String expectedBasePath = "/path/to/binaries";
      
        expect(servletConfig.getInitParameter("contentDispositionContentTypes")).andReturn(mimeTypesString);
        expect(servletConfig.getInitParameter("contentDispositionFilenameProperty")).andReturn(expectedFilenameProperty);
        expect(servletConfig.getInitParameter("baseBinariesContentPath")).andReturn(expectedBasePath);
        expect(servletConfig.getInitParameter("binaryDataPropName")).andReturn(null);
        expect(servletConfig.getInitParameter("binaryMimeTypePropName")).andReturn(null);
        expect(servletConfig.getInitParameter("binaryLastModifiedPropName")).andReturn(null);
    
        replay(servletConfig);
        binariesServlet.init(servletConfig);
        verify(servletConfig);

        Set<String> actualMimeTypes = binariesServlet.contentDispositionContentTypes;
        assertNotNull(actualMimeTypes);
        assertEquals(3, actualMimeTypes.size());
        assertTrue(actualMimeTypes.contains("application/pdf"));
        assertTrue(actualMimeTypes.contains("application/rtf"));
        assertTrue(actualMimeTypes.contains("application/excel"));

        assertEquals(expectedBasePath, binariesServlet.baseBinariesContentPath);
    }

    /**
     * Precondition: None of the possible init-params are set.
     *
     * Pass condition: No exception is thrown and the configuration fields have their default values set
     */
    @Test
    public void testInit_noParams() throws Exception {
        ServletConfig servletConfig = createMock(ServletConfig.class);

        expect(servletConfig.getInitParameter("contentDispositionContentTypes")).andReturn(null);
        expect(servletConfig.getInitParameter("contentDispositionFilenameProperty")).andReturn(null);
        expect(servletConfig.getInitParameter("baseBinariesContentPath")).andReturn(null);
        expect(servletConfig.getInitParameter("binaryDataPropName")).andReturn(null);
        expect(servletConfig.getInitParameter("binaryMimeTypePropName")).andReturn(null);
        expect(servletConfig.getInitParameter("binaryLastModifiedPropName")).andReturn(null);
       
        replay(servletConfig);
        binariesServlet.init(servletConfig);
        verify(servletConfig);

        Set<String> actualMimeTypes = binariesServlet.contentDispositionContentTypes;
        assertNotNull(actualMimeTypes);
        assertEquals(0, actualMimeTypes.size());

        assertEquals("", binariesServlet.baseBinariesContentPath);
    }

    /**
     * Precondition: - A list of content types and the name of the filename property are set.
     *               - The given response has a content type set that matches one of the content types in the list.
     *
     * Pass condition: The Content-Disposition header is added to the HTTP response, along with the filename.
     */
    @Test
    public void testSetContentDispositionHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        binariesServlet.contentDispositionContentTypes = new HashSet<String>(Arrays.asList(
                "application/pdf", "application/rtf", "application/excel"));

        binariesServlet.contentDispositionFilenamePropertyNames = new String [] { "myschema:filename" };
        String filenamePropertyName = binariesServlet.contentDispositionFilenamePropertyNames[0];

        Property filenameProperty = createMock(Property.class);
        expect(filenameProperty.getString()).andReturn("filename.pdf");

        Node binaryFileNode = createMock(Node.class);
        expect(binaryFileNode.hasProperty(filenamePropertyName)).andReturn(true);
        expect(binaryFileNode.getProperty(filenamePropertyName)).andReturn(filenameProperty);

        replay(binaryFileNode, filenameProperty);
        binariesServlet.addContentDispositionHeader(request, response, "application/pdf", binaryFileNode);
        verify(binaryFileNode, filenameProperty);
        
        Map<String, String> headerParams = MimeUtil.getHeaderParams((String) response.getHeader("Content-Disposition"));
        assertEquals("attachment", headerParams.get(""));
        assertEquals("filename.pdf", DecoderUtil.decodeEncodedWords(headerParams.get("filename")));
    }

    /**
     * Precondition: - A list of content types and the name of the filename property are set.
     *               - The given response has a content type set that matches one of the content types in the list.
     *               - The given node does not contain the configured filename property
     *
     * Pass condition: The Content-Disposition header is added to the HTTP response, without a filename.
     */
    @Test
    public void testSetContentDispositionHeader_filenamePropertyDoesNotExist() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        binariesServlet.contentDispositionContentTypes = new HashSet<String>(Arrays.asList(
                "application/pdf", "application/rtf", "application/excel"));

        binariesServlet.contentDispositionFilenamePropertyNames = new String [] { "myschema:filename" };
        String filenamePropertyName = binariesServlet.contentDispositionFilenamePropertyNames[0];

        Node binaryFileNode = createMock(Node.class);
        expect(binaryFileNode.hasProperty(filenamePropertyName)).andReturn(false);

        replay(binaryFileNode);
        binariesServlet.addContentDispositionHeader(request, response, "application/pdf", binaryFileNode);
        verify(binaryFileNode);

        assertEquals("attachment", response.getHeader("Content-Disposition"));
    }

    /**
     * Precondition: - Only a list of content types is set.
     *               - The given response has a content type set that matches one of the content types in the list.
     *
     * Pass condition: The Content-Disposition header is added to the HTTP response, without a filename.
     */
    @Test
    public void testSetContentDispositionHeader_noFilenameProperty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        binariesServlet.contentDispositionContentTypes = new HashSet<String>(Arrays.asList(
                "application/pdf", "application/rtf", "application/excel"));

        Node binaryFileNode = createMock(Node.class);

        replay(binaryFileNode);
        binariesServlet.addContentDispositionHeader(request, response, "application/pdf", binaryFileNode);
        verify(binaryFileNode);

        assertEquals("attachment", response.getHeader("Content-Disposition"));
    }

    /**
     * Precondition: - A list of content types and the name of the filename property are set.
     *               - The given response has a content type set that matches NONE of the content types in the list.
     *
     * Pass condition: The Content-Disposition header is NOT added to the HTTP response.
     */
    @Test
    public void testSetContentDispositionHeader_noMatch() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        binariesServlet.contentDispositionContentTypes = new HashSet<String>(Arrays.asList(
                "application/pdf", "application/rtf", "application/excel"));

        binariesServlet.contentDispositionFilenamePropertyNames = new String [] { "myschema:filename" };
        String filenamePropertyName = binariesServlet.contentDispositionFilenamePropertyNames[0];

        Property filenameProperty = createMock(Property.class);

        Node binaryFileNode = createMock(Node.class);

        replay(binaryFileNode, filenameProperty);
        binariesServlet.addContentDispositionHeader(request, response, "image/jpeg", binaryFileNode);
        verify(binaryFileNode, filenameProperty);

        assertFalse(response.containsHeader("Content-Disposition"));
    }
    
    /**
     * Precondition: All possible init-params are set with glob expression parameter ('application/*').
     *
     * Pass condition: Content types init-param is properly parsed as an array of Strings and the filename property is
     *                 properly retrieved from init-params.
     */
    @Test
    public void testSetContentDispositionHeaderWithGlobConfigs() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        binariesServlet.contentDispositionContentTypes = new HashSet<String>(Arrays.asList("application/*"));

        binariesServlet.contentDispositionFilenamePropertyNames = new String [] { "myschema:filename", "myschema:filename2" };

        Property filenameProperty = createMock(Property.class);
        expect(filenameProperty.getString()).andReturn("filename.pdf");

        Node binaryFileNode = createMock(Node.class);
        expect(binaryFileNode.hasProperty(binariesServlet.contentDispositionFilenamePropertyNames[0])).andReturn(false);
        expect(binaryFileNode.hasProperty(binariesServlet.contentDispositionFilenamePropertyNames[1])).andReturn(true);
        expect(binaryFileNode.getProperty(binariesServlet.contentDispositionFilenamePropertyNames[1])).andReturn(filenameProperty);

        replay(binaryFileNode, filenameProperty);
        binariesServlet.addContentDispositionHeader(request, response, "application/pdf", binaryFileNode);
        verify(binaryFileNode, filenameProperty);
        
        Map<String, String> headerParams = MimeUtil.getHeaderParams((String) response.getHeader("Content-Disposition"));
        assertEquals("attachment", headerParams.get(""));
        assertEquals("filename.pdf", DecoderUtil.decodeEncodedWords(headerParams.get("filename")));
    }
}
