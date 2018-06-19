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

import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.DEFAULT_PATH;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_TYPE;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.MANAGE_CONTENT_LINK;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.PARAMETER_NAME;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.PARAMETER_VALUE;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.PARAMETER_VALUE_IS_RELATIVE_PATH;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.PICKER_CONFIGURATION;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.PICKER_INITIAL_PATH;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.PICKER_REMEMBERS_LAST_VISITED;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.PICKER_ROOT_PATH;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.PICKER_SELECTABLE_NODE_TYPES;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.ROOT_PATH;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.TEMPLATE_QUERY;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.UUID;
import static org.hippoecm.hst.core.container.ContainerConstants.HST_COMPONENT_WINDOW;
import static org.hippoecm.hst.core.container.ContainerConstants.RENDER_VARIANT;
import static org.hippoecm.hst.utils.TagUtils.encloseInHTMLComment;
import static org.hippoecm.hst.utils.TagUtils.toJSONMap;

/**
 * This tag creates a manage content button in the channel manager.
 */
public class HstManageContentTag extends TagSupport {

    private static final Logger log = LoggerFactory.getLogger(HstManageContentTag.class);

    private final SortedMap<String, String> result = new TreeMap<>();
    private HippoBean hippoBean;

    @Override
    public int doStartTag() {
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        writeToMap(HST_TYPE, MANAGE_CONTENT_LINK);
        try {
            final HstRequestContext requestContext = RequestContextProvider.get();

            if (requestContext == null) {
                log.warn("Cannot create a manageContent button outside the hst request.");
                return EVAL_PAGE;
            }

            if (!requestContext.isCmsRequest()) {
                log.debug("Skipping manageContent tag because not in cms preview.");
                return EVAL_PAGE;
            }

            if (result.get(TEMPLATE_QUERY) == null && hippoBean == null && result.get(PARAMETER_NAME) == null) {
                log.debug("Skipping manageContent tag because neither 'templateQuery', 'hippobean' or 'parameterName' attribute specified.");
                return EVAL_PAGE;
            }

            if (hippoBean != null) {
                final String uuid = getUuid();
                if (uuid == null) {
                    return EVAL_PAGE;
                } else {
                    writeToMap(UUID, uuid);
                }
            }

            final JcrPath jcrPath = getJcrPath();
            final boolean isRelativePathParameter = jcrPath != null && jcrPath.isRelative();
            if (isRelativePathParameter && StringUtils.startsWith(result.get("rootPath"), "/")) {
                log.warn("Ignoring manageContent tag in template '{}' for component parameter '{}':"
                                + " the @{} annotation of the parameter makes it store a relative path to the"
                                + " content root of the channel while the 'rootPath' attribute of the manageContent"
                                + " tag points to the absolute path '{}'."
                                + " Either make the root path relative to the channel content root,"
                                + " or make the component parameter store an absolute path.",
                        getComponentRenderPath(), result.get(PARAMETER_NAME), JcrPath.class.getSimpleName(), result.get("rootPath"));
                return EVAL_PAGE;
            }

            final String absoluteRootPath;
            try {
                absoluteRootPath = checkRootPath(requestContext);
            } catch (final RepositoryException e) {
                log.warn("Exception while checking rootPath parameter for manageContent tag in template '{}'.",
                        getComponentRenderPath(), e);
                return EVAL_PAGE;
            }
            if (jcrPath != null) {
                writeToMap(PICKER_CONFIGURATION, jcrPath.pickerConfiguration());
                writeToMap(PICKER_INITIAL_PATH, getPickerInitialPath(jcrPath));
                writeToMap(PICKER_REMEMBERS_LAST_VISITED, Boolean.toString(jcrPath.pickerRemembersLastVisited()));

                final String pickerRootPath = getFirstNonBlankString(jcrPath.pickerRootPath(), absoluteRootPath, getChannelRootPath());
                writeToMap(PICKER_ROOT_PATH, pickerRootPath);

                final String nodeTypes = Arrays.stream(jcrPath.pickerSelectableNodeTypes()).collect(Collectors.joining(","));
                writeToMap(PICKER_SELECTABLE_NODE_TYPES, nodeTypes);
            }

            final String componentValue = getComponentValue(isRelativePathParameter);
            if (result.get(PARAMETER_NAME) != null) {
                writeToMap(PARAMETER_VALUE_IS_RELATIVE_PATH, Boolean.toString(isRelativePathParameter));
                writeToMap(PARAMETER_VALUE, componentValue);
            }
            try {
                write();
            } catch (final IOException ignore) {
                throw new JspException("manageContent tag exception in template '" + getComponentRenderPath()
                        + "': cannot write to the output writer.");
            }

            return EVAL_PAGE;
        } finally {
            result.clear();
        }
    }

    private String getUuid() {
        final HippoNode documentNode = (HippoNode) hippoBean.getNode();
        try {
            final Node editNode = documentNode.getCanonicalNode();
            if (editNode == null) {
                log.debug("Cannot create a manageContent tag, cannot find canonical node of '{}'",
                        documentNode.getPath());
                return null;
            }

            final Node handleNode = getHandleNodeIfIsAncestor(editNode);
            if (handleNode == null) {
                log.warn("Could not find handle node of {}", editNode.getPath());
                return null;
            }

            log.debug("The node path for the manageContent tag is '{}'", handleNode.getPath());
            return handleNode.getIdentifier();
        } catch (final RepositoryException e) {
            log.warn("Error while retrieving the handle of '{}', skipping manageContent tag",
                    JcrUtils.getNodePathQuietly(hippoBean.getNode()), e);
            return null;
        }
    }

    private static String getChannelRootPath() {
        final ResolvedMount resolvedMount = RequestContextProvider.get().getResolvedMount();
        return resolvedMount.getMount().getContentPath();
    }

    private String checkRootPath(final HstRequestContext requestContext) throws RepositoryException {
        final String rootPath = result.get(ROOT_PATH);

        if (rootPath == null) {
            return null;
        }

        String absoluteRootPath = getAbsoluteRootPath(requestContext);

        try {
            final Node rootPathNode = requestContext.getSession().getNode(absoluteRootPath);
            if (!rootPathNode.isNodeType(HippoStdNodeType.NT_FOLDER) && !rootPathNode.isNodeType(HippoStdNodeType.NT_DIRECTORY)) {
                log.warn("Rootpath '{}' is not a folder node. Parameters rootPath and defaultPath of manageContent tag"
                        + " in template '{}' are ignored.", rootPath, getComponentRenderPath());
                result.remove(DEFAULT_PATH);
                result.remove(ROOT_PATH);
                absoluteRootPath = null;
            }
        } catch (final PathNotFoundException e) {
            log.warn("Rootpath '{}' does not exist. Parameters rootPath and defaultPath of manageContent tag"
                    + " in template '{}' are ignored.", rootPath, getComponentRenderPath());
            result.remove(DEFAULT_PATH);
            result.remove(ROOT_PATH);
            absoluteRootPath = null;
        }
        return absoluteRootPath;
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

    private String getAbsoluteRootPath(final HstRequestContext requestContext) {
        final String rootPath = result.get(ROOT_PATH);
        if (StringUtils.startsWith(rootPath, "/")) {
            return rootPath;
        } else {
            return "/" + requestContext.getSiteContentBasePath() + "/" + rootPath;
        }
    }

    private String getComponentValue(final boolean isRelativePathParameter) {
        final String parameterName = result.get(PARAMETER_NAME);
        if (parameterName == null) {
            return null;
        }

        final HstComponentWindow window = getCurrentComponentWindow();
        final String prefixedParameterName = getPrefixedParameterName(window, parameterName);
        final String componentValue = window.getParameter(prefixedParameterName);

        if (componentValue != null && isRelativePathParameter) {
            return getChannelRootPath() + "/" + componentValue;
        }
        return componentValue;
    }

    private HstComponentWindow getCurrentComponentWindow() {
        final ServletRequest request = pageContext.getRequest();
        return (HstComponentWindow) request.getAttribute(HST_COMPONENT_WINDOW);
    }

    private String getPrefixedParameterName(final HstComponentWindow window, final String parameterName) {
        final Object parameterPrefix = window.getAttribute(RENDER_VARIANT);

        if (parameterPrefix == null || parameterPrefix.equals("")) {
            return parameterName;
        }

        return ConfigurationUtils.createPrefixedParameterName(parameterPrefix.toString(), result.get(PARAMETER_NAME));
    }

    private JcrPath getJcrPath() {
        final String parameterName = result.get(PARAMETER_NAME);
        if (parameterName == null) {
            return null;
        }

        final HstComponentWindow window = (HstComponentWindow) pageContext.getRequest().getAttribute(HST_COMPONENT_WINDOW);
        final HstComponent component = window.getComponent();
        final ComponentConfiguration componentConfig = component.getComponentConfiguration();
        final ParametersInfo paramsInfo = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(component, componentConfig);
        return ParametersInfoUtils.getParameterAnnotation(paramsInfo, parameterName, JcrPath.class);
    }

    private void write() throws IOException {
        final JspWriter writer = pageContext.getOut();
        final String comment = encloseInHTMLComment(toJSONMap(result));
        writer.print(comment);
    }

    private void writeToMap(final String key, final String value) {
        if (StringUtils.isNotEmpty(value)) {
            result.put(key, value);
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

    private static String getPickerInitialPath(final JcrPath jcrPath) {
        final String pickerInitialPath = jcrPath.pickerInitialPath();
        final String prependRootPath = getFirstNonBlankString(jcrPath.pickerRootPath(), getChannelRootPath());
        if ("".equals(pickerInitialPath) || pickerInitialPath.startsWith("/")) {
            return pickerInitialPath;
        } else {
            return prependRootPath + (prependRootPath.endsWith("/") ? "" : "/") + pickerInitialPath;
        }
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

    public void setParameterName(final String parameterName) {
        if (StringUtils.isBlank(parameterName)) {
            log.warn("The parameterName attribute of a manageContent tag in template '{}' is set to '{}'."
                    + " Expected the name of an HST component parameter instead.", getComponentRenderPath(), parameterName);
        }
        writeToMap(PARAMETER_NAME, parameterName);
    }

    public void setDefaultPath(final String defaultPath) {
        writeToMap(DEFAULT_PATH, defaultPath);
    }

    public void setHippobean(final HippoBean hippoBean) {
        this.hippoBean = hippoBean;
    }

    public void setRootPath(final String rootPath) {
        writeToMap(ROOT_PATH, rootPath);
    }

    public void setTemplateQuery(final String templateQuery) {
        if (StringUtils.isBlank(templateQuery)) {
            log.warn("The templateQuery attribute of a manageContent tag in template '{}' is set to '{}'."
                    + " Expected the name of a template query instead.", getComponentRenderPath(), templateQuery);
        }
        writeToMap(TEMPLATE_QUERY, templateQuery);
    }
}