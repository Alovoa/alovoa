$(function() {
	if (document.referrer.includes("android-app://com.alovoa.alovoa_playstore")) {
		$("#donation-links").hide();
		alert(getText("donate-list.store.warning"));
	}
});