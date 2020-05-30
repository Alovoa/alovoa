function refreshCaptcha() {
	$.get("/captcha/generate", function(data) {
		console.log(data);
		let captcha = data;
		$("#captcha").val("");
		$("#captcha-id").val(captcha.id);
		$("#captcha-image").attr("src",
				"data:image/png;base64," + captcha.image);
	});
}

refreshCaptcha();