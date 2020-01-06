var registration;

if ('serviceWorker' in navigator) {
	window.addEventListener('load', function() {
		navigator.serviceWorker.register('/js/service-worker.js').then(function(reg) {
			registration = reg;
			console.log('Registered!');
		}, function(err) {
			console.log('Service worker registration failed: ', err);
		}).catch(function(err) {
			console.log(err);
		});
	});
} else {
	console.log('service worker is not supported');
}