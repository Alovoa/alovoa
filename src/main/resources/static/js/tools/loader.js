const MIN_LOADER_TIME = 400;
var loaderTime;

function showLoader() {
	loaderTime = new Date().getTime();
	$(".loader-parent").css("display", "flex");
}

function hideLoader() {
	if(!loaderTime) {
		$(".loader-parent").css("display", "none");
	} else {
		let diffTime = new Date().getTime() - loaderTime;
		console.log(diffTime);
		if(diffTime >= MIN_LOADER_TIME) {
			$(".loader-parent").css("display", "none");
		} else {
			setTimeout(function() {
			  $(".loader-parent").css("display", "none");
			}, MIN_LOADER_TIME - diffTime);
		}
	}
}