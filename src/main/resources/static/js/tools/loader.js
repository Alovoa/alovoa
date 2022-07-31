const MIN_LOADER_TIME = 400;
var loaderTime;
var loaderNum = 0

function showLoader() {
	console.log("showLoader")
	console.log(loaderNum)
	loaderNum = loaderNum + 1
	loaderTime = new Date().getTime();
	$("#loader-parent").css("display", "flex");
}

function hideLoader() {
	loaderNum = loaderNum - 1
	console.log(loaderNum)
	if (loaderNum <= 0) {
		if(!loaderTime) {
			$("#loader-parent").css("display", "none");
		} else {
			let diffTime = new Date().getTime() - loaderTime;
			console.log(diffTime);
			if(diffTime >= MIN_LOADER_TIME) {
				console.log("hide1")
				$("#loader-parent").css("display", "none");
			} else {
				setTimeout(function() {
					console.log("hide2")
				  $("#loader-parent").css("display", "none");
				}, MIN_LOADER_TIME - diffTime);
			}
		}
	}
}