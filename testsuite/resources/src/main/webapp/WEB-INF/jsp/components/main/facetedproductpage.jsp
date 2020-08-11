<%--
  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>

<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<hst:headContribution keyHint="title"><title>${document.title}</title></hst:headContribution>

<div class="yui-u">
  
  <h2>${document.title}</h2>
  <p>
    ${document.summary}
  </p>
  
  <p>
    Product : ${document.product}
  </p>
  <p>
    Brand : ${document.brand}
  </p>
  <p>
    Type : ${document.type}
  </p>
  <p>
    Color : ${document.color}
  </p>
  <p>
    Price : ${document.price}
  </p>
  <p>
    Tags : ${fn:join(document.tags, ', ')}
  </p>
  
</div>
