<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst" prefix="h" %>
<c:set var="global" value="${context['../../../global/global']}" scope="page"/>

<div id="block">

  <div id="earmarks">
    <a href="#" onclick="window.print();" class="printpage" title="${global.linkTitle}">${global.linkText}</a>
  </div>

  <div id="menu">
    <ul>
      <c:forEach var="item" items="${context['../../../pages']}">
        <c:if test="${item['jcr:primaryType'] == 'hippo:handle'}">
          <li><c:url var="url" value="${item._path}"/><a href="${url}"/>${item[item._name].pageLabel}</a>
        </c:if>
      </c:forEach>
    </ul>
  </div>

</div>
