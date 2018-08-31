// This will control the hinting on page
var hint_map = {}
var hint_mode = false
var hints = 'abcdefghijklmnopqrstuvwxyz1234567890'.split('')

function get_hint (c, href, parent) {
  var h = document.createElement('div')

  h.style.backgroundColor = 'rgba(0,0,0,.6)'
  h.style.fontFamily = 'Iosevka, monospace'
  h.style.fontWeight = 'bold'
  h.style.padding = '3px'
  h.style.borderRadius = '3px'
  h.style.color = '#af0'
  h.style.position = 'absolute'
  h.style.left = '0'
  h.style.top = '0'
  h.innerHTML = c

  return { el: h, href, parent }
}

function hinting_on () {
  hint_map = {}
  var links = document.getElementsByTagName('a')

  for (var i = 0; i < links.length; i++) {
    var hint = i > hints.length ? '' : hints[i]
    var href = links[i].href
    var parent = links[i]

    // This could maybe break some floating links or something...
    // TODO: Maybe check existing position setting is not absolute first.
    parent.style.position = 'relative'

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
