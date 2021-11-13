function openModal(id) {
	$("#"+id).addClass("is-active");
	$(document.body).addClass("is-clipped");
}

function closeModal() {
	$('.modal').removeClass("is-active");
	$(document.body).removeClass("is-clipped");
}

function hideModal() {
	closeModal();
}