<%@ page language="java" %>
<%--
  Copyright 2017 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%@ page import="java.io.*,java.util.*" %>
<%!
private static final String DEFAULT_SWAGGER_API_URI = "/site/restservices/swagger.yaml";
private static final String SWAGGER_POM_PROPS_RESOURCE_PATH = "META-INF/maven/org.webjars/swagger-ui/pom.properties";

private volatile String swaggerVersion;

private String getSwaggerModuleVersion() throws IOException {
    String version = swaggerVersion;
    if (version == null) {
        synchronized (this) {
            version = swaggerVersion;
            if (version == null) {
                InputStream is = null;
                BufferedInputStream bis = null;
                try {
                    is = Thread.currentThread().getContextClassLoader().getResourceAsStream(SWAGGER_POM_PROPS_RESOURCE_PATH);
                    bis = new BufferedInputStream(is);
                    Properties props = new Properties();
                    props.load(bis);
                    swaggerVersion = version = props.getProperty("version");
                    props.load(bis);
                } finally {
                    if (bis != null) { bis.close(); }
                    if (is != null) { is.close(); }
                }
            }
        }
    }
    return version;
}
%>

<%
response.setHeader("Cache-Control","no-cache"); 
response.setHeader("Pragma","no-cache"); 
response.setDateHeader ("Expires", -1);

String swaggerUrl = request.getParameter("url");
if (swaggerUrl == null) {
    swaggerUrl = DEFAULT_SWAGGER_API_URI;
}
final String redirectPath = "/webjars/swagger-ui/" + getSwaggerModuleVersion() + "/index.html?url=" + swaggerUrl;
response.sendRedirect(request.getContextPath() + redirectPath);
%>
