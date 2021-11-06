$(function() {
	search();
});

function search() {
	let sort = $("#sort").val();
	let url = "/donate/search/" + sort;
	$("#main-container").load(url);
}

function onDonateClick() {
	let url = '/donate-list';
	window.open(url, '_blank').focus();
}