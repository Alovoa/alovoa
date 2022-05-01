$(function() {
	if (localStorage.getItem("twa_playstore")) {
		$("#donation-links").hide();
		alert(getText("donate-list.store.warning"));
	}
});

function donateInfo() {
	let text = document.getElementById("donate-notice-1").innerHTML;
	let text2 = document.getElementById("donate-notice-2").innerHTML;
	alert(text);
	alert(text2);
}