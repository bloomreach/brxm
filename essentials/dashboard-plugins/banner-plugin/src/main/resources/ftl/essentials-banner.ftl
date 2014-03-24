<#include "/WEB-INF/ftl/essentials/common/imports.ftl">
<#-- @ftlvariable name="banner" type="{{beansPackage}}.Banner" -->
<#if banner??>
<div class="row">
    <a href="<@hst.link hippobean=banner.link />"><img src="<@hst.link hippobean=banner.image />" alt="${banner.title}"/></a>
</div>
</#if>
