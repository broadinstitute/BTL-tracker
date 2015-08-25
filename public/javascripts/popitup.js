/**
 * Little guy to pop up a window
 * Created by nnovod on 4/21/15.
 */
function popitup(url) {
    newwindow=window.open(url, 'trckrPopup', "toolbar=yes, resizable=yes, width=75, height=100");
    if (window.focus) {newwindow.focus()}
    return false;
}
