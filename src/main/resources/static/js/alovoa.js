document.querySelector(':root').style
	.setProperty('--vh', window.innerHeight / 100 + 'px');

window.addEventListener('resize', rootResize);

function rootResize() {
	document.querySelector(':root').style
		.setProperty('--vh', window.innerHeight / 100 + 'px');
}