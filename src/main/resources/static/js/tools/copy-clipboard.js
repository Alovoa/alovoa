// https://stackoverflow.com/a/51126086/8744447
function copyClipboard(txt) {

	const successMsg = getText("success.clipboard");

	var m = document;
	txt = m.createTextNode(txt);
	var w = window;
	var b = m.body;
	b.appendChild(txt);
	if (b.createTextRange) {
		var d = b.createTextRange();
		d.moveToElementText(txt);
		d.select();
		m.execCommand('copy');
	}
	else {
		var d = m.createRange();
		var g = w.getSelection;
		d.selectNodeContents(txt);
		g().removeAllRanges();
		g().addRange(d);
		m.execCommand('copy');
		g().removeAllRanges();
	}
	txt.remove();

	alert(successMsg);
}