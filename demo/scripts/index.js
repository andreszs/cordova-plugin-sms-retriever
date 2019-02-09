// For an introduction to the Blank template, see the following documentation:
// http://go.microsoft.com/fwlink/?LinkID=397704
// To debug code on page load in Ripple or on Android devices/emulators: launch your app, set breakpoints, 
// and then run "window.location.reload()" in the JavaScript Console.
(function() {
	"use strict";

	document.addEventListener('deviceready', onDeviceReady.bind(this), false);

	function onDeviceReady() {
		// Handle the Cordova pause and resume events
		document.addEventListener('pause', onPause.bind(this), false);
		document.addEventListener('resume', onResume.bind(this), false);
		// Cordova has been loaded. Perform any initialization that requires Cordova here.

		/* Initialize plugin */
		if(typeof (SMSRetriever) === 'undefined') {
			// Error: plugin not installed
			console.warn('SMSRetriever: plugin not present');
			document.getElementById('status').innerHTML = 'Error: The plugin <strong>cordova-plugin-sms-retriever</strong> is not installed';
		} else {
			// Initialize incoming SMS event listener
			document.addEventListener('onSMSArrive', function(args) {
				document.getElementById('event').innerHTML = 'Retrieved SMS: ' + args.message;
			});

			// Bind startWatch method
			document.getElementById('startWatch').addEventListener('click', function() {
				SMSRetriever.startWatch(function(msg) {
					document.getElementById('status').innerHTML = msg;
				}, function(err) {
					document.getElementById('status').innerHTML = 'Plugin error: ' + err;
				});
			});

			// Bind getHashString method
			document.getElementById('getHashString').addEventListener('click', function() {
				SMSRetriever.getHashString(function(hash) {
					document.getElementById('status').innerHTML = 'App hash for incoming SMS: ' + hash;
					document.getElementById('txtSample').innerHTML = '<#> ABC123 is your verification code for ExampleApp. ' + hash;
				}, function(err) {
					document.getElementById('status').innerHTML = 'Plugin error: ' + err;
				});
			});

			// Bind sample app own methods
			document.getElementById('imgDonate').addEventListener('click', function() {
				window.open('http://github.com/andreszs/cordova-plugin-sms-retriever', '_system');
			});
			document.getElementById('btnWebsite').addEventListener('click', function() {
				window.open('http://github.com/andreszs/cordova-plugin-sms-retriever', '_system');
			});
			document.getElementById('smsRetrieverAPI').addEventListener('click', function() {
				window.open('https://developers.google.com/identity/sms-retriever/request', '_system');
			});
		}
	};

	function onPause() {
		// TODO: This application has been suspended. Save application state here.
	};

	function onResume() {
		// TODO: This application has been reactivated. Restore application state here.
	};

})();