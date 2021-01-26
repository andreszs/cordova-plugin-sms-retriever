cordova.define("cordova-plugin-sms-retriever-api.SMSRetriever", function(require, exports, module) {
var smsRetriever = {};

/* Start the SMS Retriever API and listen for incoming SMS + trigger onSMSArrive event when valid SMS arrives */
smsRetriever.startWatch = function(successCallback, failureCallback) {
	cordova.exec( onSuccessCallback, failureCallback, 'SMSRetriever', 'startWatch', [] );
	function onSuccessCallback(msg) {
		// This is an incoming SMS event
		successCallback(msg);
	}
};

module.exports = smsRetriever;

});
