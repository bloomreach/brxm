<%@ page language="java" %>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<table class="main">
  <tr>
    <td class="leftmenu">
      <hst:include ref="bodynav"/>
    </td>
    <td class="content">
      <hst:include ref="bodymain"/>
    </td>
    <td class="rightmenu">
      <img src="<hst:link path="/images/hippo.gif"/>"/>
    </td>
  </tr>
</table>
