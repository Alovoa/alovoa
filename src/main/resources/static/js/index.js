$(function() {
	let url = window.location.href;
	
	//TODO
	if(url.includes("?confirm-registration")) {
		alert("A confirmation email has been sent to your email address.")
	} else if(url.includes("?registration-confirm-success")) {
		alert("Confirmation succeeded, you can now login and meet new people in your area!")
	} else if(url.includes("?registration-confirm-failed")) {
		alert("Confirmation failed, please try again!")
	}
});