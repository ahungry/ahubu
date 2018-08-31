// This will control the hinting on page
var hint_map = {}
var hint_mode = false
var hints = 'abcdefghijklmnopqrstuvwxyz1234567890'.split('')

function hint_span (c) {
  return '<span class="ahubu-hint" style="display:block;font-style:none !important; font-size:16px !important; position:absolute;background:rgba(0,0,0,.7);color:#fa0;font-weight:bold;padding:4px;left:0;top:0;font-family:Iosevka,monospace;">' + c + '</span>'
}

function get_hint (c, href, parent) {
  var h = document.createElement('button')

  h.style.backgroundColor = '#000'
  h.style.color = '#af0'
  h.innerHTML = c

  return { el: h, href, parent }
}

function hinting_on () {
  hint_map = {}
  var links = document.getElementsByTagName('a')

  for (var i = 0; i < links.length; i++) {
    var hint = i > hints.length ? '' : hints[i]
    var href = links[i].href
    var parent = links[i].parentNode

    var h = get_hint(hint, href, parent)
    parent.appendChild(h.el)

    hint_map[hint] = h
  }

  setTimeout(() => { hint_mode = true }, 200)
}

function hinting_off (display) {
  Object.keys(hint_map).map((k) => {
    hint_map[k].el.remove()
  })

  hint_mode = false
}

function find_hint (c) {
  hint_mode = false

  var url = hint_map[c.toLowerCase()].href
  window.location.assign(url)

  hinting_off()
}

document.addEventListener('keyup', (e) => {
  if (!hint_mode) return

  find_hint(String.fromCharCode(e.keyCode))
})
