import FieldService from '../../channel/sidePanels/rightSidePanel/fields/field.service.js';

export function fieldServiceFactory(i: any) {
  return i.get('FieldService');
}
export const FieldServiceProvider = {
  provide: FieldService,
  useFactory: fieldServiceFactory,
  deps: ['$injector']
};
