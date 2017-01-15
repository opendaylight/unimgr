define([ 'app/cpeui/cpeui.module' ], function(cpeui) {

  Array.prototype.filterByField = function(field_name, value, to_filter_out) {
      if (to_filter_out == undefined) {
          to_filter_out = false;
      }
      return this.filter(function(item) {
          return (item[field_name] == value) != to_filter_out;
      });
  };

  cpeui.factory('Utils', function() {
      var svc = {};

      svc.randomId = function () {
          return Math.floor(Math.random() * Math.pow(2, 31));
      };
      return svc;
  });

});
