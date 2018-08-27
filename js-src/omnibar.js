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

  window.location = v.trim()
}

function key_event_handler(e) {
  switch (e.key) {
    case 'Enter':
      do_ob_op()
      break
  }
}

const ob = document.createElement('input')

ob.style.backgroundColor = '#af0'
ob.style.position = 'fixed'
ob.style.bottom = 0
ob.style.left = 0
ob.style.height = '20px'
ob.style.width = '100%'
ob.id = 'ab-ob'

document.body.appendChild(ob)
ob.addEventListener('keyup', key_event_handler)
ob.focus()
