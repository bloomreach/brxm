<#include "/WEB-INF/freemarker/include/imports.ftl">
<#--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
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
        <#if pageable.currentPage == pageNr>
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