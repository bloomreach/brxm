<#--
  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)

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
<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>

<div id="poll">
<#if pollDocument??>
    <#if pollDocument.title??>
      <h2>${pollDocument.title}</h2>
    </#if>

    <#if pollDocument.poll.text??>
      <h3>${pollDocument.poll.text}</h3>
    </#if>
  <span id="noCookieSupportMessage">The poll cannot be shown because the browser does not support cookies</span>

    <#-- Render Poll Form if not yet voted (voteSuccess not defined) or voting failed (voteSuccess is false) -->
    <#if !(voteSuccess??) || voteSuccess == false>
      <div id="pollDiv">
          <#if pollDocument.poll.introduction??>
            <p id="">${pollDocument.poll.introduction}</p>
          </#if>

        <!-- The Poll -->
        <form id="form-poll" method="post" action="<@hst.actionURL />">
          <input type="hidden" name="path" value="${path}"/>
          <div>
              <#list pollDocument.poll.options as curOption>
                <div>
                  <input id="${curOption.value}" name="option" type="radio" value="${curOption.value}"
                         <#if option?? && curOption == option>selected="true"</#if> />
                  <label for="${curOption.value}">${curOption.label}</label>
                </div>
              </#list>
          </div>
          <button class="submit" type="submit">Vote</button>
            <#if voteSuccess??> <#-- Implies voteSuccess == "false" -->
              <div>Sorry, processing the vote has failed</div>
            </#if>
        </form>
      </div>
    </#if>

  <ul id="pollResults" class="poll-results-list">
      <#list pollVotes.options as curOption>
        <li>
          <div class="poll-graph-bar">
            <span class="poll-meter" style="width: ${curOption.votesPercentage}%"> </span>
          </div>
            <#if curOption.votesCount == 1>
            ${curOption.label} - ${curOption.votesPercentage}% (${curOption.votesCount} vote)
            <#else>
            ${curOption.label} - ${curOption.votesPercentage}% (${curOption.votesCount} votes)
            </#if>
        </li>
      </#list>
      <#if pollVotes.totalVotesPercentage != 100>
        <li>
          Due to rounding the percentages don't add up to 100%
        </li>
      </#if>
  </ul>

  <script  type="text/javascript">
    if (<#if voteSuccess?? && voteSuccess == true>1<#else>0</#if>) {
      hide("noCookieSupportMessage");
    } else if (checkBrowserSupportsCookies()) {
      hide("noCookieSupportMessage");
      hide("pollResults");
    } else {
      hide("pollDiv");
      hide("pollResults");
    }

    function hide(id) {
      var element = document.getElementById(id);
      element.parentNode.removeChild(element);
    }

    function checkBrowserSupportsCookies() {
      var cookieDate=new Date();
      var cookieString="testCookieSupport"+cookieDate.toUTCString();
      document.cookie="testCookieSupport="+cookieString;
      return document.cookie.length > 0;
    }
  </script>

  <#else>
    No poll available
  </#if>
</div>
