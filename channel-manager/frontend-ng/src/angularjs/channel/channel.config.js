export function config($stateProvider) {
  'ngInject';

  $stateProvider.state('hippo-cm.channel', {
    url: '/channel/:channelId/',
    controller: 'ChannelCtrl as channelCtrl',
    templateUrl: 'channel/channel.html',
  });
}
