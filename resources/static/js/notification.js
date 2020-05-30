const PERMISSION_ALLOWED = 0;
 
function displayNotification(msg) {
	if (Notification.permission == 'granted') {
		navigator.serviceWorker.getRegistration().then(function(reg) {
			registration.showNotification(msg);
		});
	} else {
		Notification.requestPermission(function(status) {
		    if(status == PERMISSION_ALLOWED) {
		    	registration.showNotification(msg);
		    }
		});
	}
}