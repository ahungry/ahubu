//if (!document.getElementById('FirebugLite'))
//{E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;
// E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src',
//'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}

console.error('loading stuff')
var my_awesome_script = document.createElement('script');
my_awesome_script.setAttribute('src','https://getfirebug.com/firebug-lite.js#startOpened');
document.head.appendChild(my_awesome_script);
