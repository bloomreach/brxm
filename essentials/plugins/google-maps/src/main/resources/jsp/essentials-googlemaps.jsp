<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="componentId" type="java.lang.String"--%>
<%--@elvariable id="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsGoogleMapsComponentInfo"--%>

<div id="map-canvas-${componentId}" style="width: ${requestScope.cparam.width}px; height: ${requestScope.cparam.height}px;"></div>

<hst:headContribution category="htmlBodyEnd">
    <script type="text/javascript">
        if (!window.HEGM) {
            window.HEGM = [];
        }
        window.HEGM.push(function() {
            initGoogleMap('map-canvas-${componentId}', '${fn:escapeXml(requestScope.cparam.address)}', ${requestScope.cparam.longitude}, ${requestScope.cparam.latitude}, ${requestScope.cparam.zoomFactor}, '${requestScope.cparam.mapType}');
        });
    </script>
</hst:headContribution>

<hst:headContribution keyHint="essentials-google-maps" category="htmlBodyEnd">
    <hst:webfile path="/js/essentials-google-maps.js" var="essentialsGoogleMapsJs" />
    <script type="text/javascript" src="${essentialsGoogleMapsJs}"></script>
</hst:headContribution>

<hst:headContribution keyHint="google-maps-api" category="htmlBodyEnd">
    <c:choose>
        <c:when test="${not empty requestScope.cparam.apiKey}">
            <c:set var="mapsUrl">https://maps.googleapis.com/maps/api/js?key=${fn:escapeXml(requestScope.cparam.apiKey)}&callback=initGoogleMaps</c:set>
        </c:when>
        <c:otherwise>
            <c:set var="mapsUrl">https://maps.googleapis.com/maps/api/js?callback=initGoogleMaps</c:set>
        </c:otherwise>
    </c:choose>
    <script type="text/javascript" src="${mapsUrl}" async="async" defer="defer"></script>
</hst:headContribution>

<c:if test="${editMode}">
    <script type="text/javascript">
        if (window.initGoogleMap) {
            initGoogleMap('map-canvas-${componentId}', '${fn:escapeXml(requestScope.cparam.address)}', ${requestScope.cparam.longitude}, ${requestScope.cparam.latitude}, ${requestScope.cparam.zoomFactor}, '${requestScope.cparam.mapType}');
        }
    </script>
</c:if>
