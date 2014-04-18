<#include "/WEB-INF/freemarker/include/imports.ftl">
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#-- @ftlvariable name="page" type="java.lang.Integer" -->
<#if pageable??>
<ul class="pagination">
    <li class="disabled"><a href="#">${pageable.total} document(s)</a></li>
    <#list pageable.pageNumbersArray as pageNr>
        <@hst.renderURL var="pageUrl">
            <@hst.param name="page" value="${pageNr}"/>
            <@hst.param name="pageSize" value="${pageable.pageSize}"/>
        </@hst.renderURL>
        <#if (pageNr_index==0 && pageable.previous)>
            <@hst.renderURL var="pageUrlPrevious">
                <@hst.param name="page" value="${pageNr}"/>
                <@hst.param name="pageSize" value="${pageable.pageSize}"/>
            </@hst.renderURL>
            <li><a href="${pageUrlPrevious}">previous</a></li>
        </#if>
        <#if page == pageNr>
            <li class="active"><a href="#">${pageNr}</a></li>
        <#else >
            <li><a href="${pageUrl}">${pageNr}</a></li>
        </#if>

        <#if !pageNr_has_next && pageable.next>
            <@hst.renderURL var="pageUrlNext">
                <@hst.param name="page" value="${pageNr}"/>
                <@hst.param name="pageSize" value="${pageable.pageSize}"/>
            </@hst.renderURL>
            <li><a href="${pageUrlNext}">next</a></li>
        </#if>
    </#list>
</ul>
</#if>