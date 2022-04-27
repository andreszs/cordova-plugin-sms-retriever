/* global cordova */

var app = {
	RETRIEVER_STARTED: false,
	// Application Constructor
	initialize: function () {
		document.addEventListener('deviceready', this.onDeviceReady.bind(this), false);
	},
	// deviceready Event Handler
	// Bind any cordova events here. Common events are:
	// 'pause', 'resume', etc.
	onDeviceReady: function () {
		// Cordova has been loaded. Perform any initialization that requires Cordova here.
		$('#status').html('<span class="success">Device is ready</span>');

		// Get current app version
		var onSuccess = function (version) {
			$('#a_version').html('App version: ' + version);
		};
		cordova.getAppVersion.getVersionNumber(onSuccess);

		// Initialize incoming SMS event listener
		var onEvent = function (args) {
			$('#event').html('onSMSArrive: ' + args.message);
			app.RETRIEVER_STARTED = false;
		};
		document.addEventListener('onSMSArrive', onEvent);

		// Bind startWatch method
		$('#startWatch').on('click', function () {
			var onSuccess = function (strSuccess) {
				$('#status').html('<span class="success">' + strSuccess + '</span>');
				if (strSuccess == 'SMS_RETRIEVER_DONE') {
					app.RETRIEVER_STARTED = false;
				} else {
					app.RETRIEVER_STARTED = true;
				}
			};
			var onFail = function (strError) {
				$('#status').html('<span class="error">' + strError + '</span>');
				app.RETRIEVER_STARTED = false;
			};
			cordova.plugins.SMSRetriever.startWatch(onSuccess, onFail);
		});

		// Bind getHashString method
		$('#getHashString').on('click', function () {
			if (app.RETRIEVER_STARTED === true) {
				$('#status').html('Blocked action: You must NOT call this method after a retriever is started, otherwise the SMS will be ignored!');
			} else {
				var onSuccess = function (strSuccess) {
					$('#status').html('<span class="success">' + strSuccess + '</span>');
					$('#txtMessage').val('AZC123 is your code for andreszsogon.com SMS Retriever Demo App. ' + strSuccess);
					app.RETRIEVER_STARTED = false;
				};
				var onFail = function (strError) {
					$('#status').html('<span class="error">' + strError + '</span>');
					app.RETRIEVER_STARTED = false;
				};
				cordova.plugins.SMSRetriever.getHashString(onSuccess, onFail);
			}
		});

		// Bind sample app own methods
		$('#btnGitHub').on('click', function () {
			window.open('http://github.com/andreszs/cordova-plugin-sms-retriever', '_system');
		});
		$('#btnWebsite,#lblWebsite').on('click', function () {
			window.open('https://www.andreszsogon.com/cordova-sms-retriever-plugin-demo-app/', '_system');
		});
		$('#btnPlayStore').on('click', function () {
			window.open('https://play.google.com/store/apps/details?id=com.andreszs.smsretriever.demo', '_system');
		});
	}
};

app.initialize();
