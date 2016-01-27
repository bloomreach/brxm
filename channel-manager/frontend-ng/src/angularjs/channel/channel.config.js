export function config($stateProvider) {
  $stateProvider.state('hippo-cm.channel', {
    url: '/channel/:channelId/',
    controller: 'ChannelCtrl as channelCtrl',
    templateUrl: 'channel/channel.html',
  });
}
