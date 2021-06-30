$(function() {
	if (docCookies.hasItem("twa_playstore")) {
		$("#donation-links").hide();
		alert(getText("donate-list.store.warning"));
	}
});