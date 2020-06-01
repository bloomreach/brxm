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
<!DOCTYPE html>
<#include "../htmlTags.ftl">
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <@hst.headContributions categoryExcludes="scripts" xhtml=true/>

    <@hst.webfile var="link" path="/css/style.css"/>
    <link rel="stylesheet" href="${link}" type="text/css"/>

    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
</head>
<body>

<@hst.include ref="header"/>
<@hst.include ref="main"/>
<@hst.headContributions categoryIncludes="scripts" xhtml=true/>

<#include "noticefooter.ftl" ignore_missing=true />

</body>
</html>
