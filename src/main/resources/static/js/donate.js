$(function() {
	search();
});

function search() {
	let sort = $("#sort").val();
	let url = "/donate/search/" + sort;
	$("#main-container").load(url);
}

function viewProfile(idEnc) {
	let url = '/profile/view/' + idEnc;
	window.open(url, '_blank').focus();
}

function onDonateClick() {
	let url = '/donate-list';
	window.open(url, '_blank').focus();
}