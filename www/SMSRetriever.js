var exec = require('cordova/exec');

var smsRetriever = {
	startWatch: function (successCallback, failureCallback) {
		var onSuccessCallback = function(item) {
			if(typeof(item) == 'object' && typeof(item.sms) !== 'undefined'){
				// This is an incoming SMS event
				cordova.fireDocumentEvent('onSMSArrive', {'message': item.sms});
				successCallback("SMS_RETRIEVER_DONE");
			} else {
				// This is the result of the regular startWatch method
				successCallback(item);
			}
		};
		exec(onSuccessCallback, failureCallback, 'SMSRetriever', 'startWatch', []);
	},
	stopWatch: function (successCallback, failureCallback) {
		exec(successCallback, failureCallback, 'SMSRetriever', 'stopWatch', []);
	},
	getHashString: function (successCallback, failureCallback) {
		exec(successCallback, failureCallback, 'SMSRetriever', 'getHashString', []);
	},
	getPhoneNumber: function (successCallback, failureCallback) {
		exec(successCallback, failureCallback, 'SMSRetriever', 'getPhoneNumber', []);
	},
};

module.exports = smsRetriever;
