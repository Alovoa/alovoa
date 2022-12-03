$(function() {
	search(1);
	getUpdates();
});

function search(sort) {
	showLoader();
	let url = "/donate/search/" + sort;
	$("#main-container").load(url, function() {
		closeModal();
		hideLoader();
	});
}

function donateSortClicked() {
	openModal("donate-settings-modal");
}