//https://github.com/naturalprogrammer/webpush-java-demo/blob/master/src/main/resources/static/sw.js
/**
 * Created by thihara on 8/29/16.
 * 
 * The service worker for displaying push notifications.
 */
// Apache 2.0

const cacheName = 'v1';
const staticAssets = [
  './',
  '/index.html',
  '/css/lib/bulma.min.css',
  '/css/alovoa.css',
  '/css/index.css',
  
  '/sw.js',
  '/js/lib/fontawesome.all.js',
  '/js/lib/jquery.min.js',
  '/js/index.js',
  
  '/img/android-chrome-512x512.png',
  '/img/android-chrome-192x192.png',
  '/manifest.json',
  '/favicon.ico'
  
];

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

    if(clients.openWindow){
        event.waitUntil(clients.openWindow(self.clickTarget));
    }
});

self.addEventListener('install', async event => {
	  console.log('install event')
	  const cache = await caches.open(cacheName); 
	  await cache.addAll(staticAssets); 
	});

	self.addEventListener('fetch', async event => {
	  console.log('fetch event')
	});