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
      
      function intToIp(intVal) {
          var byte1 = ( intVal >>> 24 );
          var byte2 = ( intVal >>> 16 ) & 255;
          var byte3 = ( intVal >>>  8 ) & 255;
          var byte4 = intVal & 255;
          return ( byte1 + '.' + byte2 + '.' + byte3 + '.' + byte4 );
      }
      
      svc.getSubnetEdges = function(subnet) {
          var ip = subnet.split('/')[0];
          var mask = subnet.split('/')[1];
          if (mask > 30) {
              return {min:ip,
                      max:ip,
                      cidr:subnet}
          }
          var ipParts = ip.split('.');
          var ipValue = Number(ipParts[0]) * Math.pow(2,24) + Number(ipParts[1]) * Math.pow(2,16) + Number(ipParts[2]) * Math.pow(2,8) + Number(ipParts[3]);
          var minIP = intToIp((ipValue & (~(Math.pow(2,32-mask)-1))) + 1);
          var maxIP = intToIp((ipValue | ((Math.pow(2,32-mask)-1))) -1);
          var cidr = intToIp(ipValue & (~(Math.pow(2,32-mask)-1)))+"/"+mask;
          
          return {min:minIP,
                  max:maxIP,
                  cidr:cidr};
      }
      
      return svc;
  });

});
