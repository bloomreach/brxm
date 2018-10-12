<#ftl output_format="HTML">
<!DOCTYPE html>
<#--
  Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)

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
  <title>BloomReach Repository Browser - Login</title>
  <link href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAgCAYAAAAFQMh/AAAAAXNSR0IArs4c6QAAA8BJREFUSA3FV0loU1EUPe9n6GA6WMRKVLSK81Bx2Dgg1C50oTvFhVJE0UWXrsSVC0EQQURwKQiCKAgudKN1UaqoWEoHh+KAY6XUFtMxSZt8z/nJ10y12pJ44fT9f+/Lvffd6f0aTEbPe0oRCq+leD0QXwGY+YAd5FoJGyXkF8PwCQjz7xifB/jcQ3yFZb+C5e3AnEUvsMZEycsik8Zp+RREZPwwbHs3+VsJf5r8X1+MGaOuZhjrLkp817F1oZxzKGG46TNPEz3PE+2n915X6KV0e0UxNpcXoTZQhJWzfKj0elBOQbnHwgTPOxiLY3Aijr5oDJ0jUbQPRdESGsPLkXFXjbtGYMxVlPhPywGDpg8bYcce0miFu0Pr0WAZzi6pQnXRLz9SxVM+tw5GcOJ1H1qHIul7DVPh9e4wePDuGY1uSZUGPAahnTWwTHomUvf8zfPDgVHsavuWvdWYWxa5qzMlwzEbBzp78XY0K1yZW3O+27aN+/2jaOz+nlNO5iqdeJgnnjXZjtqA/3eOS5ljn4UKr4UyN8fMr/Ls5HiYOR6O4HEojK+R2GQq2Rima8oEtjvK1BFDkyuahkSh/i9kwdivC27ZRjcNexo5dZjnApExvfDhlIW6mqeAtZYJv82sx/Nm3mDcGSC+0nXYufSNGlXt9NIx2PR+EUdcA6t8D51gb9ueGTqiqnwMC/cQt66hfklvUh/bCWgm9hE/kszE8qS/HCOhDXxZz0tiOR2ZT6eC9Ho2ecXORZG4JMb4Hib6icQlYbNuvLwkAv52bA6Okp9KC/hyRQzlVz84REzZXtwzXSrmDxsJXRSdUiLDut4EzTdeFqgjioiZUikVMG24TMiga6dToZbhXJNL4esiOohuIhHGRErc8Or3OkkJUUUECd50WEUwRU79+LhmUpd+qJEUyJTk+b1Lk6stz0ZyqXdssmJ5R/6Ov5uHfK2alNXyRuGuJC4SCnu+DKrdzhCqB4dSPwLKyDlC3CS0caZOqEuuEQcJFaFLm3RaVe1e4qPLTa6SLSZUnUqHqlVVmxggCc/lmFvhctStfIVTer8QmSR9l8RUOwmnCCnNF82j4nOEPsKyBoi8v0E0EDrhTKmGCo4Tdwh9R7mp++MA4T70EgrZdAdIlZTkIGeADFKgoiokdWiAtBTSYtLWI61KupLtxj/fazNt/RrRGuQniQ9Evgy/ou5jhNo0ixT6euIC0U5MENN1RG3zjDhLbCPSKKf1lB1+Pi8j1BZKyVxC/2PprtYkklNhQkb0BaMuEN4lIcdz0k/hdpmlzPxT8gAAAABJRU5ErkJggg==" rel="icon" type="image/png" />
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
          background: #32629b url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB0AAAAYCAYAAAAGXva8AAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAWZJREFUeNrsVtFRwkAQ9WjA60A6MB2YEqACsQKxA6xArCBSQbQCsQPoIHQQKojv6QuzZC4hkwQ/HHbmzYW73L3s7ts9XFEUV39t7kJ6VjtFivUIGA/J9+Opc85Oegxz4BbYmvevAZJ/4P23Xk5aT+XVJ8eGTTMg1cf1I+UhIvQtNkZdiY/Ci5EhXeN503IzCRMgY8jNUsxzOI+zslOkKcZphy8fK9e0e+BV4x64Ab6sBg6kEk7eRyBl2DE8As/0Er9fzPKW5x9yCiTA5Azl6Ct6ick30voTcDd453Eu1yPJV8r3L6kWfdcyaEFeCmpz1JHk+mKgsDKUs1JoSl8a7EhK/Kpt2dQQJiohr5R5eTsNdiSzqakpLNQY4pBwKooNNodRYD4PNXiGTEJgWT2oFqsWqT4bLUTK0EwC0meRLykKCW9Xc/tkre5Te8uIZF7mwti7zbW5jaq2NKVS33svf1f+Fem3AAMA5G0u/dkz/GsAAAAASUVORK5CYII=) no-repeat 5px 0;
          height: 25px;
          background-position-x: 5px
      }
    </style>
  </head>
<body>
  <div class="hippo-header"></div>
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
