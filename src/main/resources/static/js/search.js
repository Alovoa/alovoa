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
			console.log(position);
		});
	} else {
		//TODO
		alert("Your browser does not support HTML5 geolocation. Please try with another browser.");
	}
}