$(function() {

	bulmaCollapsible.attach();
	let url = window.location.href;
	if (url.includes("?error")) {
		let text = getText("login.js.error");
		alert(text);
	}
});