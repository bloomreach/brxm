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
  // The following is a fix for CMS-10548 which is specific for IE11 on Windows 10.
  // Stackoverflow suggested this and after adding a magic timeout it delivered.
  setTimeout(function() {
    jQuery('use').each(function() {
      if (this.href && this.href.baseVal) {
        this.href.baseVal = this.href.baseVal;
      }
    });
  }, 10);
});
