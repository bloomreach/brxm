/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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
import alertCircleOutline from '!!raw-loader!./icons/alert-circle-outline.svg';
import alertOutline from '!!raw-loader!./icons/alert-outline.svg';
import calendarClock from '!!raw-loader!./icons/calendar-clock.svg';
import chevronRight from '!!raw-loader!./icons/chevron-right.svg';
import commentCheckOutline from '!!raw-loader!./icons/comment-check-outline.svg';
import commentProcessingOutline from '!!raw-loader!./icons/comment-processing-outline.svg';
import commentRemoveOutline from '!!raw-loader!./icons/comment-remove-outline.svg';
import lockOutline from '!!raw-loader!./icons/lock-outline.svg';
import minusCircleOutline from '!!raw-loader!./icons/minus-circle-outline.svg';
import plus from '!!raw-loader!./icons/plus.svg';
import xpageIcon from '!!raw-loader!./icons/xpage.svg';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

export function registerSvgIcons(iconRegistry: MatIconRegistry, donSanitizer: DomSanitizer): void {
  const registerIcon = (name: string, svg: string) => {
    iconRegistry.addSvgIconLiteral(name, donSanitizer.bypassSecurityTrustHtml(svg));
  };

  registerIcon('xpage', xpageIcon);
  registerIcon('minus-circle-outline', minusCircleOutline);
  registerIcon('alert-outline', alertOutline);
  registerIcon('comment-processing-outline', commentProcessingOutline);
  registerIcon('comment-remove-outline', commentRemoveOutline);
  registerIcon('calendar-clock', calendarClock);
  registerIcon('alert-circle-outline', alertCircleOutline);
  registerIcon('lock-outline', lockOutline);
  registerIcon('comment-check-outline', commentCheckOutline);
  registerIcon('plus', plus);
  registerIcon('chevron-right', chevronRight);
}
