import { MenuItemContainer } from '../main-menu/models';

import { QaHelperService } from './qa-helper.service';

describe('QaHelperService', () => {
  function setup(): {
    qaHelperService: QaHelperService;
  } {
    const qaHelperService = new QaHelperService();

    return {
      qaHelperService,
    };
  }

  it('should get the menu item class for the item', () => {
    const { qaHelperService } = setup();

    expect(qaHelperService.getMenuItemClass('my-test-class')).toEqual(
      'qa-menu-item-my-test-class',
    );

    const menuItem = new MenuItemContainer('my Awesome Caption', []);

    expect(qaHelperService.getMenuItemClass(menuItem)).toEqual(
      'qa-menu-item-my-awesome-caption',
    );
  });
});
