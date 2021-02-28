// https://github.com/naturalprogrammer/webpush-java-demo/blob/master/src/main/resources/static/js/main.js
// Apache 2.0

var notificationSubscriptionUrl = '/notification/subscribe';

// Vapid public key.
// var applicationServerPublicKey = 'BC5Ur3Wq8-2djUYGD-VSXbMByIgxFFqtXoewSOYrthgxNUTOeueIHSB2b_81UT7p0By8MoFI2Bv9hrWeB8P3k2Q';

var serviceWorkerName = '/sw.js';

// var isSubscribed = false;
var isSubscribed = (Notification.permission == "granted");
var swRegistration = null;

$(document).ready(function () {
	
	if(isSubscribed) {
		$('#btnPushNotifications').hide();
	} else {
	    $('#btnPushNotifications').click(function (event) {
	    	subscribe();
	    });
	}
});

function initialiseServiceWorker() {
    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register(serviceWorkerName).then(handleSWRegistration);
    } else {
        console.log('Service workers aren\'t supported in this browser.');
        disableAndSetBtnMessage('Service workers unsupported');
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
    navigator.serviceWorker.ready.then(function (reg) {
        // Do we already have a push message subscription?
        reg.pushManager.getSubscription()
            .then(function (subscription) {
            	
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
            .catch(function (err) {
                console.log('Error during getSubscription()', err);
            });
    });
}

function subscribe() {
	
	if(!isSubscribed) {
		Notification.requestPermission().then(function (status) {
			if(status == 'granted') {
				initialiseServiceWorker();
			}
		});
	}
}

// function unsubscribe() {
// var endpoint = null;
// swRegistration.pushManager.getSubscription()
// .then(function(subscription) {
// if (subscription) {
// endpoint = subscription.endpoint;
// return subscription.unsubscribe();
// }
// })
// .catch(function(error) {
// console.log('Error unsubscribing', error);
// })
// .then(function() {
// removeSubscriptionFromServer(endpoint);
//
// console.log('User is unsubscribed.');
// isSubscribed = false;
//
// makeButtonSubscribable(endpoint);
// });
// }

function sendSubscriptionToServer(endpoint, key, auth) {
    var encodedKey = btoa(String.fromCharCode.apply(null, new Uint8Array(key)));
    var encodedAuth = btoa(String.fromCharCode.apply(null, new Uint8Array(auth)));
    $.ajax({
        type: 'POST',
        url: notificationSubscriptionUrl,
        data: JSON.stringify({publicKey: encodedKey, auth: encodedAuth, endPoint: endpoint}),
        contentType : "application/json",
        headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
        success: function (response) {
            console.log('Subscribed successfully! ' + JSON.stringify(response));
        }
    });
}

// function removeSubscriptionFromServer(endpoint) {
// $.ajax({
// type: 'POST',
// url: '/unsubscribe',
// data: {notificationEndPoint: endpoint},
// success: function (response) {
// console.log('Unsubscribed successfully! ' + JSON.stringify(response));
// },
// dataType: 'json'
// });
// }

function disableAndSetBtnMessage(message) {
// setBtnMessage(message);
// $('#btnPushNotifications').attr('disabled','disabled');
	('#btnPushNotifications').hide();
}

function enableAndSetBtnMessage(message) {
// setBtnMessage(message);
// $('#btnPushNotifications').removeAttr('disabled');
}

function makeButtonSubscribable(reg) {
// enableAndSetBtnMessage('Subscribe to push notifications');
// $('#btnPushNotifications').addClass('btn-primary').removeClass('btn-danger');
    var subscribeParams = {userVisibleOnly: true};
    
    // Setting the public key of our VAPID key pair.
    var applicationServerKey = urlB64ToUint8Array(applicationServerPublicKey);
    subscribeParams.applicationServerKey = applicationServerKey;

    reg.pushManager.subscribe(subscribeParams)
        .then(function (subscription) {

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
        .catch(function (e) {
            // A problem occurred with the subscription.
            console.log('Unable to subscribe to push.', e);
        });
}

function makeButtonUnsubscribable() {
// enableAndSetBtnMessage('Unsubscribe from push notifications');
// $('#btnPushNotifications').addClass('btn-danger').removeClass('btn-primary');
    $('#btnPushNotifications').hide();
}

// function setBtnMessage(message) {
// $('#btnPushNotifications').text(message);
// }

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