$(function() {
	search();
});

function search() {
	let sort = $("#sort").val();
	let url = "/donate/search/" + sort;
	$("#main-container").load(url);
}

function viewProfile(idEnc) {
	let url = 'profile/view/' + idEnc + "?showHeader=false";
	loadIframe(url);
}

function onDonateClick() {
	$('#donate-modal').removeClass('is-active');
	loadIframe('donate-list' +  '?showHeader=false');
}