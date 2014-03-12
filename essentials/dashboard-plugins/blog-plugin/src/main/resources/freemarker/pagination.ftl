<#include "/WEB-INF/ftl/essentials/common/imports.ftl">
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#-- @ftlvariable name="pageSize" type="java.lang.Integer" -->
<#-- @ftlvariable name="page" type="java.lang.Integer" -->
<#if pageable??>
<ul class="pagination">
    <li class="disabled"><a href="#">${pageable.total} document(s)</a></li>
    <#list pageable.pageNumbersArray as pageNr>
        <@hst.renderURL var="pageUrl">
            <@hst.param name="page" value="${pageNr}"/>
            <@hst.param name="pageSize" value="${pageSize}"/>
        </@hst.renderURL>
        <#if test="${index.first && pageable.previous}">
            <@hst.renderURL var="pageUrlPrevious">
                <@hst.param  name="page" value=pageNr/>
                <@hst.param  name="pageSize" value=pageSize/>
            </@hst.renderURL>
            <li><a href="${pageUrlPrevious}">previous</a></li>
        </#if>
        <#if page==pageNr>
            <li class="active"><a href="#">${pageNr}</a></li>
        <#else >
            <li><a href="${pageUrl}">${pageNr}</a></li>
        </#if>
        <#if index.last && pageable.next>
            <@hst.renderURL var="pageUrlNext">
                <@hst.param name="page" value="${pageNr}"/>
                <@hst.param name="pageSize" value="${pageSize}"/>
            </@hst.renderURL>
            <li><a href="${pageUrlNext}">next</a></li>
        </#if>
    </#list>
</ul>
</#if>