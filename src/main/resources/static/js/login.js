$(function() {

	let url = window.location.href;
	
	//TODO
	if(url.includes("?error")) {
		alert("Login failed, please check your credentials, complete the captcha and try again. \n" +
				"A new confirmation email has been sent to you if your account hasn't been verified yet.")
	} 
});