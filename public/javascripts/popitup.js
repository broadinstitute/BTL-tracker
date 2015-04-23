/**
 * Little guy to pop up a window
 * Created by nnovod on 4/21/15.
 */
function popitup(url) {
    newwindow=window.open(url, 'trckrPopup', 'height=100,width=75');
    if (window.focus) {newwindow.focus()}
    return false;
}
