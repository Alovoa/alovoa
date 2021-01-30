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
			$("#main-container").load(url, function() {
				
				var sliders = [];

				$('.swiper-container').each(function(index, element){
				    $(this).addClass('s'+index);
				    let slider = new Swiper('.s'+index, { initialSlide : 1,
						shortSwipes : false});
			    	

					slider.on('transitionEnd', function () { 
						console.log(slider.activeIndex);
						console.log($(slider.el).attr('id'));
						
						let id = $(slider.el).attr("id");
						
						if(slider.activeIndex == 0) {
							likeUser(id);
						} else if(slider.activeIndex == 2) {
							hideUser(id);
						}
					 });
					 
					 sliders.push(slider);
				});
			});
			$("#filter-div").addClass("searched");

			console.log("TEST");
		}, function(error) {
			console.log(error);
		});
	} else {
		alert(getText("search.js.error.no-geolocation"));
	}
}

function viewProfile(idEnc) {
	window.open('/profile/view/' + idEnc, '_blank');
}

function likeUser(idEnc) {
	$.ajax({
		type : "POST",
		url : "/user/like/" + idEnc,
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function() {
			hideProfileTile(idEnc);
		},
		error : function(e) {
			console.log(e);
			alert(getGenericErrorText());
		}
	});

}

function hideUser(idEnc) {
	$.ajax({
		type : "POST",
		url : "/user/hide/" + idEnc,
		headers : {
			"X-CSRF-TOKEN" : $("input[name='_csrf']").val()
		},
		success : function() {
			hideProfileTile(idEnc);
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

function hideProfileTile(id) {
	let tile = $("#" + id);
	$(tile).fadeOut(200, function() {
		tile.hide();
	});
}

function getUserDivFromButton(btn) {
	return $(btn).parent().parent().parent().parent();
}
