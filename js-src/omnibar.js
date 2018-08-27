function show_ob() {
  document.getElementById('ab-ob').style.display = 'block'
  ob.focus()
}

function hide_ob() {
  ob.blur()
  document.getElementById('ab-ob').style.display = 'none'
}

function do_ob_op() {
  let v = document.getElementById('ab-ob').value.trim()

  // No dot, probably a search not URL.
  if (!/\./.test(v)) {
    v = 'https://duckduckgo.com/lite/?q=' + v + '&kp=1'
  }

  // Make sure its some type of URL.
  if (!/^http[s]:\/\//.test(v)) {
    v = 'http://' + v
  }

  document.body.style.backgroundColor = '#333'
  document.body.style.color = '#af0'
  document.body.style.fontSize = '50px'
  document.body.style.fontFamily = 'Iosevka, monospace'
  document.body.innerHTML = 'Loading...'

  window.location.assign(v.trim())
}

function key_event_handler(e) {
  // For some reason, webview does not recognize 'key' prop, only keycode
  switch (e.keyCode) {
    case 13: // Enter
      do_ob_op()
      break
    case 27: // ESC
      hide_ob()
      break
  }
}

const ob = document.createElement('input')

ob.style.display = 'block'
ob.style.backgroundColor = '#af0'
ob.style.position = 'fixed'
ob.style.top = 0
ob.style.left = 0
ob.style.height = '20px'
ob.style.width = '100%'
ob.id = 'ab-ob'

document.body.appendChild(ob)
ob.addEventListener('keyup', key_event_handler)
