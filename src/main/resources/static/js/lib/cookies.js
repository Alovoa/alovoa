/*\
|*|	:: cookies.js ::
|*|
|*|	A complete cookies reader/writer framework with full unicode support.
|*|
|*|	Revision #8 - February 18th, 2020
|*|
|*|	https://developer.mozilla.org/en-US/docs/Web/API/document.cookie
|*|	https://developer.mozilla.org/User:fusionchess
|*|	https://github.com/madmurphy/cookies.js
|*|
|*|	This framework is released under the GNU Public License, version 3 or later.
|*|	http://www.gnu.org/licenses/gpl-3.0-standalone.html
|*|
|*|	Syntaxes:
|*|	* docCookies.setItem(name, value[, end[, path[, domain[, secure[, same-site]]]]])
|*|	* docCookies.getItem(name)
|*|	* docCookies.removeItem(name[, path[, domain[, secure[, same-site]]]])
|*|	* docCookies.hasItem(name)
|*|	* docCookies.keys()
|*|	* docCookies.clear([path[, domain[, secure[, same-site]]]])
\*/
!function(){function e(e,o,t,n,r,s,i){var c="";if(t)switch(t.constructor){case Number:c=t===1/0?"; expires=Fri, 31 Dec 9999 23:59:59 GMT":"; max-age="+t;break;case String:c="; expires="+t;break;case Date:c="; expires="+t.toUTCString();break}return encodeURIComponent(e)+"="+encodeURIComponent(o)+c+(r?"; domain="+r:"")+(n?"; path="+n:"")+(s?"; secure":"")+(i&&"no_restriction"!==i.toString().toLowerCase()?"lax"===i.toString().toLowerCase()||1===Math.ceil(i)||!0===i?"; samesite=lax":"none"===i.toString().toLowerCase()||i<0?"; samesite=none":"; samesite=strict":"")}var o=/[\-\.\+\*]/g,t=/^(?:expires|max\-age|path|domain|secure|samesite|httponly)$/i;window.docCookies={getItem:function(e){return e&&decodeURIComponent(document.cookie.replace(new RegExp("(?:(?:^|.*;)\\s*"+encodeURIComponent(e).replace(o,"\\$&")+"\\s*\\=\\s*([^;]*).*$)|^.*$"),"$1"))||null},setItem:function(o,n,r,s,i,c,a){return!(!o||t.test(o))&&(document.cookie=e(o,n,r,s,i,c,a),!0)},removeItem:function(o,t,n,r,s){return!!this.hasItem(o)&&(document.cookie=e(o,"","Thu, 01 Jan 1970 00:00:00 GMT",t,n,r,s),!0)},hasItem:function(e){return!(!e||t.test(e))&&new RegExp("(?:^|;\\s*)"+encodeURIComponent(e).replace(o,"\\$&")+"\\s*\\=").test(document.cookie)},keys:function(){for(var e=document.cookie.replace(/((?:^|\s*;)[^\=]+)(?=;|$)|^\s*|\s*(?:\=[^;]*)?(?:\1|$)/g,"").split(/\s*(?:\=[^;]*)?;\s*/),o=e.length,t=0;t<o;t++)e[t]=decodeURIComponent(e[t]);return e},clear:function(e,o,t,n){for(var r=this.keys(),s=r.length,i=0;i<s;i++)this.removeItem(r[i],e,o,t,n)}}}(),"undefined"!=typeof module&&void 0!==module.exports&&(module.exports=docCookies);