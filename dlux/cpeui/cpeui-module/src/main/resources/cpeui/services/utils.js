define([ 'app/cpeui/cpeui.module' ], function(cpeui) {

  Array.prototype.filterByField = function(field_name, value, to_filter_out) {
      if (to_filter_out == undefined) {
          to_filter_out = false;
      }
      return this.filter(function(item) {
          return (item[field_name] == value) != to_filter_out;
      });
  };

  cpeui.factory('CpeUiUtils', function() {
      var svc = {};

      svc.randomId = function () {
          return Math.floor(Math.random() * Math.pow(2, 31));
      };
      
      
      svc.getSubnetEdges = function(subnet) {
          var ip = subnet.split('/')[0];
          var mask = subnet.split('/')[1];
          var ipParts = ip.split('.');
          var ipValue = Number(ipParts[0]) * Math.pow(2,24) + Number(ipParts[1]) * Math.pow(2,16) + Number(ipParts[2]) * Math.pow(2,8) + Number(ipParts[3]);
          var minIP = (ipValue & (~(Math.pow(2,32-mask)-1))) + 1;
          var byte1 = ( minIP >>> 24 );
          var byte2 = ( minIP >>> 16 ) & 255;
          var byte3 = ( minIP >>>  8 ) & 255;
          var byte4 = minIP & 255;
          minIP =  ( byte1 + '.' + byte2 + '.' + byte3 + '.' + byte4 );
          
          var maxIP = (ipValue | ((Math.pow(2,32-mask)-1))) -1;
          byte1 = ( maxIP >>> 24 );
          byte2 = ( maxIP >>> 16 ) & 255;
          byte3 = ( maxIP >>>  8 ) & 255;
          byte4 = maxIP & 255;
          maxIP =  ( byte1 + '.' + byte2 + '.' + byte3 + '.' + byte4 );
          
          return [minIP, maxIP];
          
      }
      
      return svc;
  });

});
