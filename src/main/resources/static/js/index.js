$(function() {
	let url = window.location.href;
	
	//TODO
	if(url.includes("?confirm-registration")) {
		alert("A confirmation email has been sent to your email address.")
	} else if(url.includes("?registration-confirm-success")) {
		alert("Confirmation succeeded, you can now login and meet new people in your area!")
	} else if(url.includes("?registration-confirm-failed")) {
		alert("Confirmation failed, please try again!")
	} else if(url.includes("?password-reset-requested")) {
		alert("An email for resetting your password has been sent, please check your emails and follow the instructions.")
	} else if(url.includes("?password-change-success")) {
		alert("Your password has been successfully changed!")
	} else if(url.includes("?confirm-account-deleted")) {
		alert("Your account has been successfully deleted!")
	}
});