import { Injectable } from '@angular/core';

import { MenuItem } from '../main-menu/models';

const prefix = 'qa';

@Injectable({
  providedIn: 'root',
})
export class QaHelperService {
  getMenuItemClass(item: MenuItem | string): string {
    if (typeof item === 'string') {
      return `${prefix}-${item}`;
    }

    const classNamesList: string[] = [];

    classNamesList.push(`menu-item-${this.hyphenate(item.caption)}`);

    return classNamesList.map(className => `${prefix}-${className}`).join(' ');
  }

  private hyphenate(value: string): string {
    return value.toLowerCase().split(' ').join('-');
  }
}
