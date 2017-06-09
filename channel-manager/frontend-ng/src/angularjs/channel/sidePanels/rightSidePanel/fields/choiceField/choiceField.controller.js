/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class ChoiceFieldCtrl {
  getFieldName(index) {
    const choiceId = this.fieldValues[index].chosenId;
    return index > 0 ? `${this.name}[${index + 1}]/${choiceId}` : `${this.name}/${choiceId}`;
  }

  focusChoice() {
    this.hasFocus = true;
    this.onFieldFocus();
  }

  blurChoice() {
    delete this.hasFocus;
    this.onFieldBlur();
  }
}

export default ChoiceFieldCtrl;
