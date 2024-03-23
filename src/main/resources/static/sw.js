/**
 * Created by thihara on 8/29/16.
 * 
 * The service worker for displaying push notifications.
 * 
 * https://github.com/thihara/web_push_notifications/blob/master/static/sw.js
 * Apache 2.0
 */


const cacheName = '62';
const staticAssets = [

	'/favicon.ico',
	'/manifest/manifest.json',

	'/js/lib/bulma-collapsible.min.js',
	'/js/lib/bulma-slider.min.js',
	'/js/lib/bulma-toast.min.js',
	'/js/lib/fontawesome.all.js',
	'/js/lib/jquery.min.js',
	'/js/lib/jquery-ui.min.js',
	'/js/lib/matter.min.js',
	'/js/lib/modal-fx.min.js',

    '/js/tools/check-password.js',
	'/js/tools/get-text.js',
	'/js/tools/loader.js',
	'/js/tools/modal.js',
	'/js/tools/refresh-captcha.js',

	'/js/alovoa.js',
	'/js/delete-account.js',
	'/js/delete-account-request.js',
	'/js/donate-list.js',
	'/js/imprint.js',
	'/js/index.js',
	'/js/login.js',
	'/js/password-change.js',

	'/css/lib/bulma.min.css',
	'/css/lib/bulma-collapsible.min.css',
	'/css/lib/bulma-slider.min.css',
	'/css/lib/bulma-switch.min.css',
	'/css/lib/css-loaders.css',
	'/css/lib/swiper.min.css',
	'/css/lib/leaflet.min.css',
	'/css/lib/modal-fx.min.css',
	'/css/lib/animate.min.css',

	'/css/alovoa.css',
	'/css/donate-list.css',
	'/css/index.css',
	'/css/privacy.css',

	'/img/android-chrome-192x192.png',
	'/img/android-chrome-512x512.png',
	'/img/apple-touch-icon.png',
	'/img/share.png',
	'/img/m-icon.svg',
	'/img/r-icon.svg',
	'/img/t-icon.svg',
	'/img/github.svg',
	'/img/github-dark.svg',
	'/img/icon.png',
	
	'/img/profile/1.png',
	'/img/profile/2.png',
	'/img/profile/3.png',
	'/img/profile/4.png',
	'/img/profile/5.png',
	'/img/profile/6.png',
	'/img/profile/7.png',
	'/img/profile/8.png',
	'/img/profile/9.png',
	'/img/profile/10.png'
	
];

if ('serviceWorker' in navigator) {
	console.log('Loading Service Worker...')
	window.addEventListener('load', function() {
		navigator.serviceWorker.register('/sw.js').then(function(registration) {
			// Registration was successful
			console.log('ServiceWorker registration successful with scope: ', registration.scope);

			caches.keys().then(function(names) {
				for (let name of names) {
					if (name != cacheName)
						caches.delete(name);
				}
			});

		}, function(err) {
			// registration failed :(
			console.log('ServiceWorker registration failed: ', err);
		});
	});
}

self.addEventListener('push', function(event) {
	if (!(self.Notification && self.Notification.permission === 'granted')) {
		return;
	}

	var data = {};
	if (event.data) {
		data = event.data.json();
	}
	var title = data.title;
	var message = data.message;
	var icon = "img/android-chrome-512x512.png";

	self.clickTarget = data.clickTarget;

	event.waitUntil(self.registration.showNotification(title, {
		body: message,
		tag: 'Alovoa',
		icon: icon,
		badge: icon
	}));
});

self.addEventListener('notificationclick', function(event) {
	console.log('[Service Worker] Notification click Received.');

	event.notification.close();

	if (clients.openWindow) {
		event.waitUntil(clients.openWindow(self.clickTarget));
	}
});

self.addEventListener('install', async event => {
	console.log('install event')
	// Perform install steps
	event.waitUntil(
		caches.open(cacheName)
			.then(function(cache) {
				console.log('Opened cache');
				return cache.addAll(staticAssets);
			})
	);
});

self.addEventListener('fetch', async event => {
	event.respondWith(
		caches.match(event.request).then(function(response) {
			return response || fetch(event.request);
		})
	);
});