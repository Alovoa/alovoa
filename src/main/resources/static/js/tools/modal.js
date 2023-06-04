function openModal(id) {
	$("#"+id).addClass("is-active");
	$(document.body).addClass("is-clipped");
}

function closeModal(id) {
    if(id) {
        $('#'+id).removeClass("is-active");
	}
	else {
		$('.modal').removeClass("is-active");
	}

	$(document.body).removeClass("is-clipped");
}

function hideModal() {
	closeModal();
}