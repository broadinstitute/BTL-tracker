/**
 * Little guy to pop up a window
 * Created by nnovod on 4/21/15.
 */
function popitup(url) {
    newwindow=window.open(url, 'trckrPopup', "toolbar=yes, resizable=yes, top=500, left=500, width=200, height=200");
    if (window.focus) {newwindow.focus()}
    return false;
}
