<%--
  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>

<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst' %>

<hst:headContribution keyHint="title"><title>${document.title}</title></hst:headContribution>
<hst:element name="script" var="yui3Elem">
    <hst:attribute name="type" value="text/javascript"/>
    <hst:attribute name="src" value="http://yui.yahooapis.com/3.2.0/build/yui/yui-min.js"/>
</hst:element>
<hst:headContribution keyHint="yui3" element="${yui3Elem}"/>

<style type="text/css">
    .form_error {
        color: red;
    }
</style>
<div id="<hst:namespace/>detailPane" class="yui-u">
    <h1>Form Component Example</h1>
    <%--@elvariable id="form" type="org.hippoecm.hst.component.support.forms.FormMap"--%>
    <form name="hstForm" action="<hst:actionURL />" method="post">

        <input type="hidden" name="previous" value="${form.previous}"/>
        <br/>
        <table>
            <tr>

                <td>Name</td>
                <td><input type="text" name="name" value="<c:out value="${form.value['name'].value}"/>"/></td>
                <td class="form_error">${form.message['name']}</td>
            </tr>
            <tr>
                <td>Email</td>
                <td><input type="text" name="email" value="<c:out value="${form.value['email'].value}"/>"/></td>
                <td class="form_error">${form.message['email']}</td>
            </tr>
            <tr>
                <td>Text</td>
                <td><textarea name="textarea" rows="4" cols="40"><c:out value="${form.value['textarea'].value}"/></textarea></td>
                <td class="form_error">${form.message['textarea']}</td>
            </tr>
            <tr>
                <td>Checkbox</td>
                <td>
                    <p>
                        <input type="checkbox" name="checkbox"  ${empty form.value['checkbox'].values["Red"] ? '':'checked="checked"'} value="Red"> Red
                    </p>

                    <p>
                        <input type="checkbox" name="checkbox"  ${empty form.value['checkbox'].values["White"] ? '':'checked="checked"'} value="White"> White
                    </p>

                    <p>
                        <input type="checkbox" name="checkbox"  ${empty form.value['checkbox'].values["Blue"] ? '':'checked="checked"'} value="Blue"> Blue
                    </p>

                </td>
                <td class="form_error">
                </td>
            </tr>
            <tr>
                <td>Seal form:</td>
                <td>
                    <p>
                        <input type="checkbox" name="seal" value="true"> Seal form data
                    </p>
                </td>
                <td class="form_error">
                </td>
            </tr>
            <tr>
                <td class="form_error">
                    <c:if test="${not empty form.message['checkbox']}">
                        <p>First message: </p> ${form.message['checkbox']}
                        <p>All messages:</p>
                        <c:forEach var="msg" items="${form.messages['checkbox']}" varStatus="index">
                            <p>${index.index} : ${msg}</p>
                        </c:forEach>
                    </c:if>
                </td>
                <td>
                </td>
            </tr>
            <tr>
                <td>
                    <c:if test="${form.previous != null}">
                        <input type="submit" name="prev" value="prev"/>
                    </c:if>
                </td>
                <td><input type="submit" value="send"/></td>
            </tr>
        </table>
    </form>

    </form>
</div>
