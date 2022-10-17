const MIN_LOADER_TIME = 400;
var loaderTime;
var loaderNum = 0

function showLoader() {
	loaderNum = loaderNum + 1
	loaderTime = new Date().getTime();
	$("#loader-parent").css("display", "flex");
}

function hideLoader() {
	loaderNum = loaderNum - 1
	if (loaderNum <= 0) {
		if(!loaderTime) {
			$("#loader-parent").css("display", "none");
		} else {
			let diffTime = new Date().getTime() - loaderTime;
			if(diffTime >= MIN_LOADER_TIME) {
				$("#loader-parent").css("display", "none");
			} else {
				setTimeout(function() {
				  $("#loader-parent").css("display", "none");
				}, MIN_LOADER_TIME - diffTime);
			}
		}
	}
}