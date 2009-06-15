<%@ page language="java" %>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<div id="bd">
  <div id="yui-main">
    <div class="yui-b">
      <div class="yui-gf">
          <hst:include ref="leftmenu"/>
          <hst:include ref="bodymain"/>
        </div>
    </div>
  </div>
  <div role="complementary" class="yui-b">
      Second Navigation
      (empty)
  </div>
</div>