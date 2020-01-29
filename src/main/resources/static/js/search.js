var locationFound = false;

$(function() {
	$("#max-distance-slider").change(function(e) {

		let data = $("#max-distance-slider").val();
		if (data) {
			$("#max-distance-display").html(data);
		}
	});
});

function search() {
	if (navigator.geolocation) {
		navigator.geolocation.getCurrentPosition(function(position) {
			// console.log(position);
			let distance = $("#max-distance-slider").val();
			let sort = $("#sort").val();
			let url = "/search/users/" + position.coords.latitude + "/"
					+ position.coords.longitude + "/" + distance + "/" + sort;
			$("#main-container").load(url);

		});
	} else {
		// TODO
		alert("Your browser does not support HTML5 geolocation. Please try with another browser.");
	}
}

function viewProfile(idEnc) {
	window.open('/profile/view/' + idEnc, '_blank');
}

function likeUser(btn, idEnc) {
	$.ajax({
		type : "POST",
		url : "/user/like/" + idEnc,
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function() {
			hideProfileTile(btn);
		},
		error : function(e) {
			// TODO
			console.log(e);
		}
	});

}

function hideUser(btn, idEnc) {
	$.ajax({
		type : "POST",
		url : "/user/hide/" + idEnc,
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function() {
			hideProfileTile(btn);
		},
		error : function(e) {
			// TODO
			console.log(e);
		}
	});
}

function hideProfileTile(btn) {
	let parent = getUserDivFromButton(btn);
	$(parent).fadeOut(500, function() {
		parent.hide();
	});
}

function getUserDivFromButton(btn) {
	return $(btn).parent().parent().parent().parent();
}
