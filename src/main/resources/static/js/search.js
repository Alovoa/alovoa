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
	console.log("search");

	if (navigator.geolocation) {
		navigator.geolocation.getCurrentPosition(function(position) {
			console.log(position);
			let distance = $("#max-distance-slider").val();
			let sort = $("#sort").val();
			let url = "/search/users/" + position.coords.latitude + "/"
					+ position.coords.longitude + "/" + distance + "/" + sort;
			$("#main-container").load(url);
			$("#filter-div").addClass("searched");
		});
	} else {
		alert(getText("search.js.error.no-geolocation"));
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
			console.log(e);
			alert(getGenericErrorText());
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
			console.log(e);
			alert(getGenericErrorText());
		}
	});
}

var cardContentVisible = false;
function toggleCardContent() {
	
	let width = document.documentElement.clientWidth;
	
	if(cardContentVisible) {
		$(".content-background").removeClass("hidden");
		$(".profile-bottom").addClass("dimmed");
	} else {
		if(width < 1024) {
			$(".content-background").addClass("hidden");
			$(".profile-bottom").removeClass("dimmed");
		}
	}
	cardContentVisible = !cardContentVisible;
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
