<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>

<div id="main">
    <div id="leftmenu">
      <hst:include ref="leftmenu"/>
    </div>
    <div id="content">
      <hst:include ref="content"/>
      <!-- the lists is a general 'slot' where items can be dropped in -->
      <hst:include ref="lists"/>
    </div>
    <div id="right">
      <hst:include ref="right"/>
    </div>
</div>
