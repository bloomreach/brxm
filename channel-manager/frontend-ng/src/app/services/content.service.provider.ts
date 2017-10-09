import ContentService from './content.service.js';

export function contentServiceFactory(i: any) {
  return i.get('ContentService');
}
export const ContentServiceProvider = {
  provide: ContentService,
  useFactory: contentServiceFactory,
  deps: ['$injector']
};
