// https://github.com/naturalprogrammer/webpush-java-demo/blob/master/src/main/resources/static/js/main.js
// Apache 2.0

var notificationSubscriptionUrl = '/notification/subscribe';

var serviceWorkerName = '/sw.js';

var isSubscribed = (Notification.permission == "granted");
var swRegistration = null;

$(document).ready(function() {

	if (isSubscribed || !isPushApiSupported()) {
		// $('#webpush-button').toggle();
	} else {
		$('#webpush-button').toggle();
		$('#webpush-button').click(function(event) {
			subscribe();
		});
	}
});

function initialiseServiceWorker(callback) {
	if ('serviceWorker' in navigator) {
		navigator.serviceWorker.register(serviceWorkerName).then(handleSWRegistration);
		if(callback) callback();
	} else {
		console.log('Service workers aren\'t supported in this browser.');
		disableAndSetBtnMessage('Service workers unsupported');
		if(callback) callback();
	}
};

function handleSWRegistration(reg) {
	if (reg.installing) {
		console.log('Service worker installing');
	} else if (reg.waiting) {
		console.log('Service worker installed');
	} else if (reg.active) {
		console.log('Service worker active');
	}

	swRegistration = reg;
	initialiseState(reg);
}

// Once the service worker is registered set the initial state
function initialiseState(reg) {
	// Are Notifications supported in the service worker?
	if (!(reg.showNotification)) {
		console.log('Notifications aren\'t supported on service workers.');
		disableAndSetBtnMessage('Notifications unsupported');
		return;
	}

	// Check if push messaging is supported
	if (!('PushManager' in window)) {
		console.log('Push messaging isn\'t supported.');
		disableAndSetBtnMessage('Push messaging unsupported');
		return;
	}

	// We need the service worker registration to check for a subscription
	navigator.serviceWorker.ready.then(function(reg) {
		// Do we already have a push message subscription?
		reg.pushManager.getSubscription()
			.then(function(subscription) {

				if (!subscription) {
					console.log('Not yet subscribed to Push');

					isSubscribed = false;
					makeButtonSubscribable(reg);
				} else {
					// initialize status, which includes setting UI elements for
					// subscribed status
					// and updating Subscribers list via push
					isSubscribed = true;
					makeButtonUnsubscribable();
				}
			})
			.catch(function(err) {
				console.log('Error during getSubscription()', err);
			});
	});
}

function subscribe(callback) {
	if(!isPushApiSupported() || isSubscribed) {
		if(callback) callback();
	} else {
		Notification.requestPermission().then(function(status) {
			if (status == 'granted') {
				initialiseServiceWorker(callback);
			} else {
				if(callback) callback();
			}
		});
	}
}

function sendSubscriptionToServer(endpoint, key, auth) {
	var encodedKey = btoa(String.fromCharCode.apply(null, new Uint8Array(key)));
	var encodedAuth = btoa(String.fromCharCode.apply(null, new Uint8Array(auth)));
	$.ajax({
		type: 'POST',
		url: notificationSubscriptionUrl,
		data: JSON.stringify({ publicKey: encodedKey, auth: encodedAuth, endPoint: endpoint }),
		contentType: "application/json",
		success: function(response) {
			console.log('Subscribed successfully! ' + JSON.stringify(response));
		}
	});
}

function disableAndSetBtnMessage(message) {
	$('#webpush-button').toggle();
}

function makeButtonSubscribable(reg) {
	var subscribeParams = { userVisibleOnly: true };

	// Setting the public key of our VAPID key pair.
	let applicationServerPublicKey = $("#vapidPublicKey").val();
	// console.log(applicationServerPublicKey)
	var applicationServerKey = urlB64ToUint8Array(applicationServerPublicKey);
	subscribeParams.applicationServerKey = applicationServerKey;

	reg.pushManager.subscribe(subscribeParams)
		.then(function(subscription) {

			// Update status to subscribe current user on server, and to
			// let
			// other users know this user has subscribed
			var endpoint = subscription.endpoint;
			var key = subscription.getKey('p256dh');
			var auth = subscription.getKey('auth');
			sendSubscriptionToServer(endpoint, key, auth);
			isSubscribed = true;
			makeButtonUnsubscribable();
		})
		.catch(function(e) {
			// A problem occurred with the subscription.
			console.log('Unable to subscribe to push.', e);
		});
}

function makeButtonUnsubscribable() {
	$('#webpush-button').toggle();
}

function urlB64ToUint8Array(base64String) {
	const padding = '='.repeat((4 - base64String.length % 4) % 4);
	const base64 = (base64String + padding)
		.replace(/\-/g, '+')
		.replace(/_/g, '/');

	const rawData = window.atob(base64);
	const outputArray = new Uint8Array(rawData.length);

	for (var i = 0; i < rawData.length; ++i) {
		outputArray[i] = rawData.charCodeAt(i);
	}
	return outputArray;
}

function isPushApiSupported() {
	return 'PushManager' in window;
}