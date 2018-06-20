/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.tag;

import java.io.IOException;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.util.ParametersInfoAnnotationUtils;
import org.hippoecm.hst.util.ParametersInfoUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.MANAGE_CONTENT_DEFAULT_PATH;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_TYPE;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_TYPE_MANAGE_CONTENT_LINK;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.MANAGE_CONTENT_PARAMETER_NAME;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.MANAGE_CONTENT_PARAMETER_VALUE;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.MANAGE_CONTENT_PARAMETER_VALUE_IS_RELATIVE_PATH;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.MANAGE_CONTENT_PICKER_CONFIGURATION;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.MANAGE_CONTENT_PICKER_INITIAL_PATH;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.MANAGE_CONTENT_PICKER_REMEMBERS_LAST_VISITED;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.MANAGE_CONTENT_PICKER_ROOT_PATH;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.MANAGE_CONTENT_PICKER_SELECTABLE_NODE_TYPES;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.MANAGE_CONTENT_ROOT_PATH;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.MANAGE_CONTENT_TEMPLATE_QUERY;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.MANAGE_CONTENT_UUID;
import static org.hippoecm.hst.core.container.ContainerConstants.HST_COMPONENT_WINDOW;
import static org.hippoecm.hst.core.container.ContainerConstants.RENDER_VARIANT;
import static org.hippoecm.hst.utils.TagUtils.encloseInHTMLComment;
import static org.hippoecm.hst.utils.TagUtils.toJSONMap;

/**
 * This tag creates a manage content button in the channel manager.
 */
public class HstManageContentTag extends TagSupport {

    private static final Logger log = LoggerFactory.getLogger(HstManageContentTag.class);

    private HippoBean hippoBean;
    private String templateQuery;
    private String parameterName;
    private String rootPath;
    private String defaultPath;

    private HstRequestContext requestContext;
    private JcrPath jcrPath;
    private final SortedMap<String, String> result = new TreeMap<>();

    public void setHippobean(final HippoBean hippoBean) {
        this.hippoBean = hippoBean;
    }

    public void setTemplateQuery(final String templateQuery) {
        if (StringUtils.isBlank(templateQuery)) {
            log.warn("The templateQuery attribute of a manageContent tag in template '{}' is set to '{}'."
                    + " Expected the name of a template query instead.", getComponentRenderPath(), templateQuery);
        }
        this.templateQuery = templateQuery;
    }

    public void setParameterName(final String parameterName) {
        if (StringUtils.isBlank(parameterName)) {
            log.warn("The parameterName attribute of a manageContent tag in template '{}' is set to '{}'."
                    + " Expected the name of an HST component parameter instead.", getComponentRenderPath(), parameterName);
        }
        this.parameterName = parameterName;
    }

    public void setDefaultPath(final String defaultPath) {
        this.defaultPath = defaultPath;
    }

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public int doStartTag() {
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            requestContext = RequestContextProvider.get();
            jcrPath = getJcrPath();

            checkCmsRequest();
            checkMandatoryParameters();
            checkRootPath();

            write(HST_TYPE, HST_TYPE_MANAGE_CONTENT_LINK);
            processHippoBean();
            processTemplateQuery();
            processParameterName();
            processPaths();

            generateHtmlComment();
        } catch (SkipManageContentTagException e) {
            log.debug(e.getMessage());
        } catch (ManageContentTagException e) {
            log.warn(e.getMessage());
        } finally {
            requestContext = null;
            jcrPath = null;
            result.clear();
        }
        return EVAL_PAGE;
    }

    private JcrPath getJcrPath() {
        if (parameterName == null) {
            return null;
        }

        final HstComponentWindow window = (HstComponentWindow) pageContext.getRequest().getAttribute(HST_COMPONENT_WINDOW);
        final HstComponent component = window.getComponent();
        final ComponentConfiguration componentConfig = component.getComponentConfiguration();
        final ParametersInfo paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(component, componentConfig);
        return ParametersInfoUtils.getParameterAnnotation(paramsInfo, parameterName, JcrPath.class);
    }

    private void checkCmsRequest() throws ManageContentTagException, SkipManageContentTagException {
        if (requestContext == null) {
            throw new ManageContentTagException("Cannot create a manageContent button outside the hst request.");
        }
        if (!requestContext.isCmsRequest()) {
            throw new SkipManageContentTagException("Skipping manageContent tag because not in cms preview.");
        }
    }

    private void checkMandatoryParameters() throws SkipManageContentTagException {
        if (templateQuery == null && hippoBean == null && parameterName == null) {
            throw new SkipManageContentTagException("Skipping manageContent tag because neither 'templateQuery', " +
                    "'hippobean' or 'parameterName' attribute specified.");
        }
    }

    private void checkRootPath() throws ManageContentTagException {
        if (rootPath == null) {
            return;
        }

        if (isRelativePathParameter() && StringUtils.startsWith(rootPath, "/")) {
            final String warning = String.format("Ignoring manageContent tag in template '%s' for component parameter '%s':"
                            + " the @%s annotation of the parameter makes it store a relative path to the"
                            + " content root of the channel while the 'rootPath' attribute of the manageContent"
                            + " tag points to the absolute path '%s'."
                            + " Either make the root path relative to the channel content root,"
                            + " or make the component parameter store an absolute path.",
                    getComponentRenderPath(), parameterName, JcrPath.class.getSimpleName(), rootPath);
            throw new ManageContentTagException(warning);
        }

        final String absoluteRootPath = getAbsoluteRootPath();

        try {
            final Node rootPathNode = requestContext.getSession().getNode(absoluteRootPath);
            if (!rootPathNode.isNodeType(HippoStdNodeType.NT_FOLDER) && !rootPathNode.isNodeType(HippoStdNodeType.NT_DIRECTORY)) {
                log.warn("Rootpath '{}' is not a folder node. Parameters rootPath and defaultPath of manageContent tag"
                        + " in template '{}' are ignored.", rootPath, getComponentRenderPath());
                rootPath = defaultPath = null;
            }
        } catch (final PathNotFoundException e) {
            log.warn("Rootpath '{}' does not exist. Parameters rootPath and defaultPath of manageContent tag"
                    + " in template '{}' are ignored.", rootPath, getComponentRenderPath());
            rootPath = defaultPath = null;
        } catch (RepositoryException e) {
            throw new ManageContentTagException("Exception while checking rootPath parameter for manageContent tag " +
                    "in template '" + getComponentRenderPath() + "'.", e);
        }
    }

    private boolean isRelativePathParameter() {
        return jcrPath != null && jcrPath.isRelative();
    }

    private void processHippoBean() throws ManageContentTagException, SkipManageContentTagException {
        if (hippoBean == null) {
            return;
        }

        final HippoNode documentNode = (HippoNode) hippoBean.getNode();
        try {
            final Node editNode = documentNode.getCanonicalNode();
            if (editNode == null) {
                throw new SkipManageContentTagException("Cannot create a manageContent tag, " +
                        "cannot find canonical node of '" + documentNode.getPath() + "'");
            }

            final Node handleNode = getHandleNodeIfIsAncestor(editNode);
            if (handleNode == null) {
                throw new ManageContentTagException("Could not find handle node of " + editNode.getPath());
            }

            log.debug("The node path for the manageContent tag is '{}'", handleNode.getPath());
            write(MANAGE_CONTENT_UUID, handleNode.getIdentifier());
        } catch (final RepositoryException e) {
            throw new ManageContentTagException("Error while retrieving the handle of '"
                    + JcrUtils.getNodePathQuietly(hippoBean.getNode()) + "', skipping manageContent tag", e);
        }
    }

    private void processTemplateQuery() {
        write(MANAGE_CONTENT_TEMPLATE_QUERY, templateQuery);
    }

    private void processParameterName() {
        write(MANAGE_CONTENT_PARAMETER_NAME, parameterName);

        if (jcrPath != null) {
            write(MANAGE_CONTENT_PICKER_CONFIGURATION, jcrPath.pickerConfiguration());
            write(MANAGE_CONTENT_PICKER_INITIAL_PATH, getPickerInitialPath(jcrPath));
            write(MANAGE_CONTENT_PICKER_REMEMBERS_LAST_VISITED, Boolean.toString(jcrPath.pickerRemembersLastVisited()));

            final String pickerRootPath = getFirstNonBlankString(jcrPath.pickerRootPath(), getAbsoluteRootPath(), getChannelRootPath());
            write(MANAGE_CONTENT_PICKER_ROOT_PATH, pickerRootPath);

            final String nodeTypes = Arrays.stream(jcrPath.pickerSelectableNodeTypes()).collect(Collectors.joining(","));
            write(MANAGE_CONTENT_PICKER_SELECTABLE_NODE_TYPES, nodeTypes);
        }

        if (parameterName != null) {
            final String parameterValue = getParameterValue();
            write(MANAGE_CONTENT_PARAMETER_VALUE, parameterValue);
            write(MANAGE_CONTENT_PARAMETER_VALUE_IS_RELATIVE_PATH, Boolean.toString(isRelativePathParameter()));
        }
    }

    private void processPaths() {
        write(MANAGE_CONTENT_ROOT_PATH, rootPath);
        write(MANAGE_CONTENT_DEFAULT_PATH, defaultPath);
    }

    private String getAbsoluteRootPath() {
        if (rootPath == null) {
            return null;
        }

        if (StringUtils.startsWith(rootPath, "/")) {
            return rootPath;
        } else {
            return "/" + requestContext.getSiteContentBasePath() + "/" + rootPath;
        }
    }

    private String getComponentRenderPath() {
        final HstComponentWindow window = getCurrentComponentWindow();
        if (window == null) {
            return "";
        }
        final HstComponent component = window.getComponent();
        if (component == null) {
            return "";
        }
        final ComponentConfiguration componentConfiguration = component.getComponentConfiguration();
        if (componentConfiguration == null) {
            return "";
        }
        return componentConfiguration.getRenderPath();
    }

    private String getParameterValue() {
        if (parameterName == null) {
            return null;
        }

        final HstComponentWindow window = getCurrentComponentWindow();
        final String prefixedParameterName = getPrefixedParameterName(window);
        final String parameterValue = window.getParameter(prefixedParameterName);

        if (parameterValue != null && isRelativePathParameter()) {
            return getChannelRootPath() + "/" + parameterValue;
        }
        return parameterValue;
    }

    private HstComponentWindow getCurrentComponentWindow() {
        final ServletRequest request = pageContext.getRequest();
        return (HstComponentWindow) request.getAttribute(HST_COMPONENT_WINDOW);
    }

    private String getPrefixedParameterName(final HstComponentWindow window) {
        final Object parameterPrefix = window.getAttribute(RENDER_VARIANT);

        if (parameterPrefix == null || parameterPrefix.equals("")) {
            return parameterName;
        }

        return ConfigurationUtils.createPrefixedParameterName(parameterPrefix.toString(), parameterName);
    }

    private String getPickerInitialPath(final JcrPath jcrPath) {
        final String pickerInitialPath = jcrPath.pickerInitialPath();
        final String prependRootPath = getFirstNonBlankString(jcrPath.pickerRootPath(), getChannelRootPath());
        if ("".equals(pickerInitialPath) || pickerInitialPath.startsWith("/")) {
            return pickerInitialPath;
        } else {
            return prependRootPath + (prependRootPath.endsWith("/") ? "" : "/") + pickerInitialPath;
        }
    }

    private String getChannelRootPath() {
        final ResolvedMount resolvedMount = requestContext.getResolvedMount();
        return resolvedMount.getMount().getContentPath();
    }

    private void write(final String key, final String value) {
        if (StringUtils.isNotEmpty(value)) {
            result.put(key, value);
        }
    }

    private void generateHtmlComment() throws JspException {
        try {
            final JspWriter writer = pageContext.getOut();
            final String comment = encloseInHTMLComment(toJSONMap(result));
            writer.print(comment);
        } catch (final IOException ignore) {
            throw new JspException("manageContent tag exception in template '" + getComponentRenderPath()
                    + "': cannot write to the output writer.");
        }
    }

    /**
     * Get the first String that is not blank from a number of Strings.
     *
     * @param strings variable list of Strings
     * @return first non-null and not empty String or null if all are blank
     */
    private static String getFirstNonBlankString(final String... strings) {
        return Arrays.stream(strings).filter(StringUtils::isNotBlank).findFirst().orElse(null);
    }

    /*
     * when a currentNode is of type hippo:handle, we return this node, else we check the parent, until we are at the jcr root node.
     * When we hit the jcr root node, we return null;
     */
    private static Node getHandleNodeIfIsAncestor(final Node currentNode) throws RepositoryException {
        final Node rootNode = currentNode.getSession().getRootNode();
        return getHandleNodeIfIsAncestor(currentNode, rootNode);
    }

    private static Node getHandleNodeIfIsAncestor(final Node currentNode, final Node rootNode) throws RepositoryException {
        if (currentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
            return currentNode;
        }
        if (currentNode.isSame(rootNode)) {
            return null;
        }
        return getHandleNodeIfIsAncestor(currentNode.getParent(), rootNode);
    }

    private static class ManageContentTagException extends Exception {
        ManageContentTagException(final String message) {
            super(message);
        }

        ManageContentTagException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    private static class SkipManageContentTagException extends Exception {
        SkipManageContentTagException(final String message) {
            super(message);
        }
    }
}