$(function() {
	if (localStorage.getItem("twa_playstore")) {
		$("#donation-links").hide();
		alert(getText("donate-list.store.warning"));
	}
});