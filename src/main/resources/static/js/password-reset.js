$(function() {

	refreshCaptcha();
	let url = window.location.href;

	if (url.includes("?error")) {
		alert(getGenericErrorText());
	}

	$("#form").submit(function(e) {
		e.preventDefault();
		let captchaId = $("#captcha-id").val();
		let captchaText = $("#captcha").val();
		let email = $("#email").val();
		let url = "/password/reset";

		let data = {};
		data.captchaId = captchaId;
		data.captchaText = captchaText;
		data.email = email;

		$.ajax({
			type : "POST",
			url : url,
			data : JSON.stringify(data),
			contentType : "application/json",
			success : function() {
				location.href = "/?password-reset-requested";
			},
			error : function(e) {
				location.href = "/password/reset?error";
			}
		});

	});
});