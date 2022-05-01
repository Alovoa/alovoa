$(function() {
	search();
});

function search() {
	let sort = $("#sort").val();
	let url = "/donate/search/" + sort;
	$("#main-container").load(url, function() {
		closeModal();
	});
}

function donateSortClicked() {
	openModal("donate-settings-modal");
}