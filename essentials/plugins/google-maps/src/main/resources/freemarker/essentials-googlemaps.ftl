<#include "../include/imports.ftl">

<#-- @ftlvariable name="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsGoogleMapsComponentInfo" -->
<#if cparam.apiKey?has_content>
    <#assign mapsUrl = "https://maps.googleapis.com/maps/api/js?key=${cparam.apiKey?html}"/>
<#else>
    <#assign mapsUrl = "https://maps.googleapis.com/maps/api/js"/>
</#if>

<@hst.headContribution keyHint="api" category="htmlHead">
<script type="text/javascript" src="${mapsUrl}"></script>
</@hst.headContribution>

<@hst.headContribution keyHint="maps" category="htmlHead">
    <@hst.webfile path="/js/essentials-google-maps.js" var="customMapsJSUrl" />
<script type="text/javascript" src="${customMapsJSUrl}"></script>
</@hst.headContribution>

<@hst.headContribution keyHint="initializeGoogleMaps" category="htmlBodyEnd">
<script type="text/javascript">
    initializeGoogleMaps("${cparam.address?html}", ${cparam.longitude}, ${cparam.latitude}, ${cparam.zoomFactor}, "${cparam.mapType}");
</script>
</@hst.headContribution>

<#if cmsrequest>
<script type="text/javascript">
    initializeGoogleMaps("${cparam.address?html}", ${cparam.longitude}, ${cparam.latitude}, ${cparam.zoomFactor}, "${cparam.mapType}");
</script>
</#if>

<div id="map-canvas" style="width: ${cparam.width}px; height: ${cparam.height}px;"></div>