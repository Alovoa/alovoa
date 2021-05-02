function refreshCaptcha() {
	$.get("/captcha/generate", function(data) {
		let captcha = data;
		$("#captcha").val("");
		$("#captcha-id").val(captcha.id);
		$("#captcha-image").attr("src",
				"data:image/webp;base64," + captcha.image);
	});
}

refreshCaptcha();