<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--
  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
  --%>

<div id="feedback"></div>
<div id="content"></div>

<hst:headContribution keyHint="requirejs">
  <script type="text/javascript"
          data-main="<hst:link path="/js/plugins/main.js"/>"
          src="<hst:link path="/webjars/requirejs/2.1.5/require.js"/>"></script>
</hst:headContribution>