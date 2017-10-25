import SidePanelService from './sidePanel.service.js';

export function SidePanelServiceFactory(i: any) {
  return i.get('SidePanelService');
}

export const SidePanelServiceProvider = {
  provide: SidePanelService,
  useFactory: SidePanelServiceFactory,
  deps: ['$injector']
};
