/*!
 * Copyright 2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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
import accountCircleOutline from '!!raw-loader!./icons/account-circle-outline.svg';
import alertCircleOutline from '!!raw-loader!./icons/alert-circle-outline.svg';
import alertFilled from '!!raw-loader!./icons/alert-filled.svg';
import alertOutline from '!!raw-loader!./icons/alert-outline.svg';
import calendarClock from '!!raw-loader!./icons/calendar-clock.svg';
import calendarToday from '!!raw-loader!./icons/calendar-today.svg';
import cellphoneLink from '!!raw-loader!./icons/cellphone-link.svg';
import cellphone from '!!raw-loader!./icons/cellphone.svg';
import chartPie from '!!raw-loader!./icons/chart-pie.svg';
import chevronRight from '!!raw-loader!./icons/chevron-right.svg';
import close from '!!raw-loader!./icons/close.svg';
import commentCheckOutline from '!!raw-loader!./icons/comment-check-outline.svg';
import commentProcessingOutline from '!!raw-loader!./icons/comment-processing-outline.svg';
import commentRemoveOutline from '!!raw-loader!./icons/comment-remove-outline.svg';
import cookieOutline from '!!raw-loader!./icons/cookie-outline.svg';
import copy from '!!raw-loader!./icons/copy.svg';
import exitToApp from '!!raw-loader!./icons/exit-to-app.svg';
import faceOutline from '!!raw-loader!./icons/face-outline.svg';
import homeOutline from '!!raw-loader!./icons/home-outline.svg';
import iconView from '!!raw-loader!./icons/icon-view.svg';
import listView from '!!raw-loader!./icons/list-view.svg';
import lockOutline from '!!raw-loader!./icons/lock-outline.svg';
import minusCircleOutline from '!!raw-loader!./icons/minus-circle-outline.svg';
import monitor from '!!raw-loader!./icons/monitor.svg';
import plus from '!!raw-loader!./icons/plus.svg';
import sync from '!!raw-loader!./icons/sync.svg';
import tablet from '!!raw-loader!./icons/tablet.svg';
import trash from '!!raw-loader!./icons/trash.svg';
import web from '!!raw-loader!./icons/web.svg';
import xpage from '!!raw-loader!./icons/xpage.svg';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

export function registerSvgIcons(iconRegistry: MatIconRegistry, domSanitizer: DomSanitizer): void {
  const registerIcon = (name: string, svg: string) => {
    iconRegistry.addSvgIconLiteral(name, domSanitizer.bypassSecurityTrustHtml(svg));
  };

  registerIcon('account-circle-outline', accountCircleOutline);
  registerIcon('alert-circle-outline', alertCircleOutline);
  registerIcon('alert-filled', alertFilled);
  registerIcon('alert-outline', alertOutline);
  registerIcon('calendar-clock', calendarClock);
  registerIcon('calendar-today', calendarToday);
  registerIcon('cellphone-link', cellphoneLink);
  registerIcon('cellphone', cellphone);
  registerIcon('chart-pie', chartPie);
  registerIcon('chevron-right', chevronRight);
  registerIcon('close', close);
  registerIcon('comment-check-outline', commentCheckOutline);
  registerIcon('comment-processing-outline', commentProcessingOutline);
  registerIcon('comment-remove-outline', commentRemoveOutline);
  registerIcon('cookie-outline', cookieOutline);
  registerIcon('copy', copy);
  registerIcon('exit-to-app', exitToApp);
  registerIcon('face-outline', faceOutline);
  registerIcon('home-outline', homeOutline);
  registerIcon('icon-view', iconView);
  registerIcon('list-view', listView);
  registerIcon('lock-outline', lockOutline);
  registerIcon('minus-circle-outline', minusCircleOutline);
  registerIcon('monitor', monitor);
  registerIcon('plus', plus);
  registerIcon('tablet', tablet);
  registerIcon('trash', trash);
  registerIcon('web', web);
  registerIcon('xpage', xpage);
  registerIcon('sync', sync);
}
