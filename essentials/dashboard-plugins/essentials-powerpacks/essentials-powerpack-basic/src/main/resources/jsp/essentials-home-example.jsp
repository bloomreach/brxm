<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="hst" uri="http://www.hippoecm.org/jsp/hst/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>

<h1>Home Page</h1>
<p>Go directly to:</p>
<ul>
  <li><a href="<hst:link path="/news"/>">News List Page</a></li>
  <li><a href="<hst:link path="/events"/>">Events List Page</a></li>
</ul>
