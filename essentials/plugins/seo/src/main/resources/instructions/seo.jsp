<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<c:if test="${not empty requestScope.title}">
  <hst:headContribution category="SEO" keyHint="hst.seo.document.title">
    <title><c:out value="${requestScope.title}"/></title>
  </hst:headContribution>
</c:if>

<c:if test="${not empty requestScope.metaDescription}">
  <hst:headContribution category="SEO" keyHint="hst.seo.document.description">
    <meta name="description" content="${fn:escapeXml(requestScope.metaDescription)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${not empty requestScope.dublinCoreSchemaLink}">
  <hst:headContribution category="SEO" keyHint="hst.seo.schema.dc">
    <link rel="schema.DC" href="${fn:escapeXml(requestScope.dublinCoreSchemaLink)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${not empty requestScope.dublinCoreTermsLink}">
  <hst:headContribution category="SEO" keyHint="hst.seo.dc.terms">
    <link rel="schema.DCTERMS" href="${fn:escapeXml(requestScope.dublinCoreTermsLink)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${not empty requestScope.dublinCoreCopyrightLink}">
  <hst:headContribution category="SEO" keyHint="hst.seo.dc.copyright">
    <link rel="DC.rights copyright" href="${fn:escapeXml(requestScope.dublinCoreCopyrightLink)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${not empty requestScope.dublinCoreLanguage}">
  <hst:headContribution category="SEO" keyHint="hst.seo.dc.language">
    <meta scheme="DCTERMS.RFC3066" name="DC.language" content="${fn:escapeXml(requestScope.dublinCoreLanguage)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${not empty requestScope.dublinCoreTermsCreated}">
  <hst:headContribution category="SEO" keyHint="hst.seo.dc.terms.created">
    <meta name="DCTERMS.created" content="${fn:escapeXml(requestScope.dublinCoreTermsCreated)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${not empty requestScope.dublinCoreTermsModified}">
  <hst:headContribution category="SEO" keyHint="hst.seo.dc.terms.modified">
    <meta name="DCTERMS.modified" content="${fn:escapeXml(requestScope.dublinCoreTermsModified)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${hstRequest.requestContext.cmsRequest}">
  <div>
    <img src="<hst:link path='/images/essentials/catalog-component-icons/seo.png'/>"> Click to edit SEO parameters
  </div>
</c:if>
