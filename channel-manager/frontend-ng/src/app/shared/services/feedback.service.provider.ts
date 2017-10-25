import FeedbackService from '../../services/feedback.service.js';

export function feedbackServiceFactory(i: any) {
  return i.get('FeedbackService');
}
export const FeedbackServiceProvider = {
  provide: FeedbackService,
  useFactory: feedbackServiceFactory,
  deps: ['$injector']
};
