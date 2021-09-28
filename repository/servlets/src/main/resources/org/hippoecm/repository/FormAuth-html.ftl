<#ftl output_format="HTML">
<!DOCTYPE html>
<#--
  Copyright 2014-2021 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

${response.setContentType("text/html;charset=UTF-8")}

<html>
<head>
  <title>Bloomreach Repository Browser - Login</title>
  <link href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAA7lJREFUWAm9V2lI1EEUf7ua4dplh2WJlJ1CdApthxqUmUiXn4KSqOiAgqAo8EN+iiKCsOhDfYiig4guKDtIkA5UMBKygsLO1dZuK492t12333Ob+c/O7uYetQ9+zHtv3pv3Zv7zZuZvor+TCd05wDIgHxgFpANdgB14AVQC14CPwD+lxRjtEeANA79gcwzg5GKmfhjhMhBOYN2mA36rYskgE86PQwa3pHyl4RlNNCDVTmazO6Qd0d5wk+BvLIhnXgdMFoqedtiIN1S6tYZyi7zUf1Bf2edxe6ix3knnjo6hhpo8qTeYXWAPGGJwTk2Al32FNDOZu2ndjgu0chORyWSW+mDMi6edtGtNIf1oU/dAN0yLgNvBXIROJMAb7qZQYnk9tO/EGZo5zyJ1vTGd7U7aWGylD+/GKabPwfOK8ucKSjwzTmK/X+/W8vMRBWfnlP596fClWkq2fFfGmgh+vSIHsJwA1/kU2ZM59gktLe0j5UiYIcOSae32G5pLrwnwIWPQlvKHhhAFt6zUTJaUNsWTJzhSkf1YXoF8qemT1EXTrMZOlx0RMAmJZsrJe6B48CcOViU9JpwAH68+yhjTRAmJCUKMup0++7Pma8TQOjgBo3SGjlCXTjONQExL13e9EUMbhhPgi8VHLkd0m0/4i9bjEZxonYLRW06AbzUffWodKtiY2pbXSZr/J02WIifAV6qPWpsnEB8osVJdNd8pKjWqgspzApVS4fWa6NbFRClHw7xv6aInD62KK0+oVpH9WE6AHxPGRztVUUAuF9/v0dHBsizydvO4gviOcQhBb9mQXzLHZUdnxxAq3xiybKRdMOb+rZ/UUJurdVVosp8oap5Pv82Ab/PYbaOp1WajuYscuAnFheXnGCDcue6gPdtW4x2j2p+B3ZEAW0UhEuiA7i1QIvtePRtPNVVumja7hQamhi7P9m8OOrR7IJ2sKNGWnofaAtjkmEEYNVvu3guU+dmZTF7Knl5PBSte0qSpLkodbCany0vNL4nuXk+j6soF5HHrZSeG4M+7EOBXVti0E5a8KfX3XrQyH8szwo7+x3AR2mdApEFPw2cD4NJ8v0FWSxNi78TnAd5jVA/w8ypUMlxiZ4FZgKAlYFiv+rRDDrgV9T0gBtBbLksuL27TAZ4hH6/831AH/AR0KoTiCpCsdPC9sxyoUnT/lZ2P0Xnm6krwyhQDcaM5iMTvRDUJXkGj5OOQSg5ifNGS+BXvJKYiIJ8L6krYIceVshGNg4okcJrFn7IQ8ipwD7D+BrZbLK6dz8HeAAAAAElFTkSuQmCC" rel="icon" type="image/png" />
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <style type="text/css">
      h3 {margin:2px}
      table.params {font-size:small}
      td.header {text-align: left; vertical-align: top; padding: 10px;}
      td {text-align: left}
      th {text-align: left}
      * {font-family: tahoma, arial, sans-serif, helvetica;}
      * {font-size: 14px;}
      body { background-color: #efefef }
      form {margin-top: 10px}
      div.message {font-size: small; color: red}
      .hippo-header {
        height: 31px;
        background-color: #C8C8C8;
      }
      .hippo-header .logo {
        background: #ffffff url("data:image/svg+xml;utf8,<svg width='28px' height='28px' viewBox='0 0 28 28' version='1.1' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'><g stroke='none' stroke-width='1' fill='none' fill-rule='evenodd'><circle fill= '%23FFD500' fill-rule='nonzero' cx='14' cy='14' r='14'></circle><path fill= '%23002840' fill-rule='nonzero' d='M16.9363368,11.2224064 L16.242042,12.9724775 C16.9930774,13.4509726 17.4401201,14.2740357 17.4254991,15.1513741 C17.4254991,16.5126346 16.2986809,17.6161542 14.9086802,17.6161542 C13.5186796,17.6161542 12.3918614,16.5126346 12.3918614,15.1513741 C12.3619709,14.496338 12.6006638,13.856774 13.0548257,13.3749987 C13.5089877,12.8932234 14.1409396,12.6092067 14.8100588,12.5861484 L15.4609603,10.8090342 C14.2023973,10.6080828 12.9172847,10.9594377 11.9470069,11.7697628 C10.9767291,12.5800879 10.4195119,13.7673503 10.4233577,15.0161589 L10.4233577,15.0161589 L10.4233577,19.4782609 L12.3958062,19.4782609 L12.3958062,18.7674152 C13.1478968,19.2347016 14.021984,19.4788937 14.9126251,19.4705343 C16.9905273,19.4985133 18.8105189,18.1118772 19.2938096,16.1325471 C19.7771002,14.1532171 18.7945029,12.1102816 16.9284471,11.2146798 L16.9363368,11.2224064 Z'></path><path fill= '%23002840' fill-rule='nonzero' d='M10.4233577,10.7536232 C11.0064842,10.1996182 11.7009686,9.75811003 12.4671533,9.4543104 L12.4671533,6.69565217 L10.4233577,6.69565217 L10.4233577,10.7536232 Z'></path></g></svg>") no-repeat 0px 0px/23px 23px;
        border-radius: 50%;
        width: 23px;
        height: 23px;
        float: left;
        margin: 5px;
      }
    </style>
  </head>
<body>
  <div class="hippo-header">
    <div class="logo"></div>
  </div>
  <table summary="infotable">
      <tr>
          <td class="header">
              <h3>Log in</h3>
              <div class="message">${message}</div>
                <form method="post" action="" accept-charset="UTF-8">
                    <table style="params" summary="searching">
                        <tr>
                            <th>Username: </th>
                            <td>
                                  <input name="username" type="text" size="15" autofocus="autofocus"/>
                              </td>
                          </tr>
                        <tr>
                            <th>Password: </th>
                            <td>
                                <input name="password" type="password" size="15"/>
                              </td>
                          </tr>
                        <tr>
                            <th>&nbsp;</th>
                            <td>
                                <input type="submit" value="Log in"/>
                              </td>
                          </tr>
                      </table>
                  </form>
              </td>
          </tr>
      </table>
  </body>
</html>
