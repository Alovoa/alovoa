$(function() {
	search();
});

function search(sort) {
	let url = "/donate/search/" + sort;
	$("#main-container").load(url, function() {
		closeModal();
	});
}

function donateSortClicked() {
	openModal("donate-settings-modal");
}