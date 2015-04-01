<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsGoogleMapsComponentInfo"--%>
<c:choose>
  <c:when test="${not empty cparam.apiKey}">
    <c:set var="mapsUrl">https://maps.googleapis.com/maps/api/js?key=${cparam.apiKey}</c:set>
  </c:when>
  <c:otherwise>
    <c:set var="mapsUrl">https://maps.googleapis.com/maps/api/js</c:set>
  </c:otherwise>
</c:choose>

<hst:headContribution keyHint="api" category="htmlHead">
  <script type="text/javascript" src="${mapsUrl}"></script>
</hst:headContribution>

<hst:headContribution keyHint="maps" category="htmlHead">
  <hst:webfile path="/js/essentials-google-maps.js" var="customMapsJSUrl" />
  <script type="text/javascript" src="${customMapsJSUrl}"></script>
</hst:headContribution>

<hst:headContribution keyHint="initializeGoogleMaps" category="htmlBodyEnd">
  <script type="text/javascript">
    initializeGoogleMaps("${cparam.address}", ${cparam.longitude}, ${cparam.latitude}, ${cparam.zoomFactor}, "${cparam.mapType}");
  </script>
</hst:headContribution>

<c:if test="${cmsrequest}">
  <script type="text/javascript">
    updateEssentialsGoogleMap("${cparam.address}", ${cparam.longitude}, ${cparam.latitude}, ${cparam.zoomFactor}, "${cparam.mapType}");
  </script>
</c:if>

<div id="map-canvas" style="width: ${cparam.width}px; height: ${cparam.height}px;"></div>