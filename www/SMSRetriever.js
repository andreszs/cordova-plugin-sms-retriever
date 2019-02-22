var smsRetriever = {};

/* Start the SMS Retriever API and listen for incoming SMS + trigger onSMSArrive event when valid SMS arrives */
smsRetriever.startWatch = function(successCallback, failureCallback) {
	cordova.exec( onSuccessCallback, failureCallback, 'SMSRetriever', 'startWatch', [] );
	function onSuccessCallback(msg) {
		var action = msg.substr(0, 3);
		if (action === '<#>') {
			// This is an incoming SMS event
			smsRetriever.fireOnSmsArrive(msg);
			successCallback("SMS arrived; Watching stopped");
		} else {
			// This is the result of the regular startWatch method
			successCallback(msg);
		}
	}
};

/* Get the SMS Retriever API app's hash string */
smsRetriever.getHashString = function(successCallback, failureCallback) {
	cordova.exec( successCallback, failureCallback, 'SMSRetriever', 'getHashString', [] );
};

/* Event to retrieve the incoming validation SMS content */
smsRetriever.fireOnSmsArrive = function (message) {
    cordova.fireDocumentEvent('onSMSArrive', {
        'message': message
    });
};

module.exports = smsRetriever;
