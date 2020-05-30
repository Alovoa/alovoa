$(function() {
	search();
});

function search() {
	let sort = $("#sort").val();
	let url = "/donate/search/" + sort;
	$("#main-container").load(url);
}

function viewProfile(idEnc) {
	window.open('/profile/view/' + idEnc, '_blank');
}