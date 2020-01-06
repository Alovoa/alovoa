self.addEventListener('install', function() {
	console.log('Install!');
});
self.addEventListener("activate", event => {
	console.log('Activate!');
});
self.addEventListener('fetch', function(event) {
	console.log('Fetch!', event.request);
});