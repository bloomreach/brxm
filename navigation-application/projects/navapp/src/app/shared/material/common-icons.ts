/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

// tslint:disable:match-default-export-name
import arrowDropDown from '!!raw-loader!./icons/arrow_drop_down.svg';
import arrowRight from '!!raw-loader!./icons/arrow_right.svg';
import chevronDown from '!!raw-loader!./icons/chevron_down.svg';
import chevronRight from '!!raw-loader!./icons/chevron_right.svg';
import chevronUp from '!!raw-loader!./icons/chevron_up.svg';
import expandLess from '!!raw-loader!./icons/expand_less.svg';
import expandMore from '!!raw-loader!./icons/expand_more.svg';
import navCollapse from '!!raw-loader!./icons/nav-collapse.svg';
import navExpand from '!!raw-loader!./icons/nav-expand.svg';
import remove from '!!raw-loader!./icons/remove.svg';
import search from '!!raw-loader!./icons/search.svg';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

export const registerIcons = (iconRegistry: MatIconRegistry, donSanitizer: DomSanitizer) => {
  const registerIcon = (name: string, svg: string) => {
    iconRegistry.addSvgIconLiteral(name, donSanitizer.bypassSecurityTrustHtml(svg));
  };

  registerIcon('nav-collapse', navCollapse);
  registerIcon('nav-expand', navExpand);
  registerIcon('expand_less', expandLess);
  registerIcon('expand_more', expandMore);
  registerIcon('remove', remove);
  registerIcon('chevron_down', chevronDown);
  registerIcon('chevron_right', chevronRight);
  registerIcon('chevron_up', chevronUp);
  registerIcon('search', search);
  registerIcon('arrow_drop_down', arrowDropDown);
  registerIcon('arrow_right', arrowRight);
};
