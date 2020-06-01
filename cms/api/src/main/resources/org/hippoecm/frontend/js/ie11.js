/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

jQuery(document).ready(function() {
  // Fix rendering of inline SVGs in IE11 on Windows 10. This is a known bug that Microsoft won't fix anymore:
  // https://connect.microsoft.com/IE/feedback/details/2337112/svg-use-elements-disappear-on-windows-10
  setTimeout(function() {
    jQuery('use').each(function() {
      if (this.href && this.href.baseVal) {
        this.href.baseVal = this.href.baseVal;
      }
    });
  }, 10);
});
