import DialogService from '../../services/dialog.service.js';

export function dialogServiceFactory(i: any) {
  return i.get('DialogService');
}
export const DialogServiceProvider = {
  provide: DialogService,
  useFactory: dialogServiceFactory,
  deps: ['$injector']
};
