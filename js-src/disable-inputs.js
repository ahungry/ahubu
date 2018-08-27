function disableForm() {
  var inputs = document.getElementsByTagName("input");
  for (var i = 0; i < inputs.length; i++) {
    if (inputs[i].id !== 'ab-ob') {
      inputs[i].disabled = true;
      inputs[i].blur()
    }
  }
  var selects = document.getElementsByTagName("select");
  for (var i = 0; i < selects.length; i++) {
    selects[i].disabled = true;
    selects[i].blur()
  }
  var textareas = document.getElementsByTagName("textarea");
  for (var i = 0; i < textareas.length; i++) {
    textareas[i].disabled = true;
    textareas[i].blur()
  }
  var buttons = document.getElementsByTagName("button");
  for (var i = 0; i < buttons.length; i++) {
    buttons[i].disabled = true;
    buttons[i].blur()
  }
}
// disableForm()
