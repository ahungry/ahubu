function show_ob () {
  document.getElementById('ab-ob').style.display = 'block'
}

function hide_ob () {
  document.getElementById('ab-ob').style.display = 'none'
}

const ob = document.createElement('div')

ob.style.display = 'none'
ob.style.backgroundColor = 'rgba(0,0,0,.5)'
ob.style.color = 'lime'
ob.style.fontFamily = 'Iosevka, monospace'
ob.style.fontSize = '40px'
ob.style.fontWeight = 'bold'
ob.style.position = 'fixed'
ob.style.top = 0
ob.style.left = 0
ob.style.height = '100%'
ob.style.padding = '30px'
ob.style.width = '100%'
ob.id = 'ab-ob'
ob.zIndex = '8888'

document.body.appendChild(ob)
ob.addEventListener('keyup', key_event_handler)

hide_ob()
