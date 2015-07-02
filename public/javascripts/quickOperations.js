/**
 * Simple mini-forms to do quick operations
 * Created by nnovod on 4/21/15.
 */
$('#qRegister').keypress(function (e) {
    var key = e.which;
    if(key == 13 && $("#qRegister").val().trim() != "") {
        window.location.assign("/add/" + $("#qRegister").val().trim());
        return false;
    }});
$('#qFind').keypress(function (e) {
    var key = e.which;
    if(key == 13 && $("#qFind").val().trim() != "") {
        window.location.assign("/find/" + $("#qFind").val().trim());
        return false;
    }});
function doTran(e, from, to, proj) {
    var params;
    var key = e.which;
    if(key == 13 && (from != "" || to != "" || proj != ""))  {
        if(from != "") { params = "?fromID=" + from; }
        if(to != "") { if(params) { params = params + "&toID=" + to; } else { params = "?toID=" + to; } }
        if(proj != "") { if(params) { params = params + "&project=" + proj; } else { params = "?project=" + proj; } } }
    return params;
}
$('#fromTran').keypress(function (e) {
    var p = doTran(e,$("#fromTran").val().trim(),$("#toTran").val().trim(),$("#projTran").val().trim());
    if (p) { window.location.assign("/transfer" + p); return false; }
});
$('#toTran').keypress(function (e) {
    var p = doTran(e,$("#fromTran").val().trim(),$("#toTran").val().trim(),$("#projTran").val().trim());
    if (p) { window.location.assign("/transfer" + p); return false; }
});
$('#projTran').keypress(function (e) {
    var p = doTran(e,$("#fromTran").val().trim(),$("#toTran").val().trim(),$("#projTran").val().trim());
    if (p) { window.location.assign("/transfer" + p); return false; }
});
