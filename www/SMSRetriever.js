var smsExport = {};

smsExport.startWatch = function(successCallback, failureCallback) {
	cordova.exec( successCallback, failureCallback, 'SMSRetriever', 'startWatch', [] );
};

smsExport.stopWatch = function(successCallback, failureCallback) {
	cordova.exec( successCallback, failureCallback, 'SMSRetriever', 'stopWatch', [] );
};

module.exports = smsExport;
