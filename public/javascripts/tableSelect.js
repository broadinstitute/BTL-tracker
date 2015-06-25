/**
 * Created by nnovod on 6/24/15.
 * Select cells from a table
 */
/**
 * Function to select cells from a table.  A click on a cell selects just that single cell if no click modifiers are
 * present, selects from the last single cell (un)selected to the newly selected cell if a shift/click.
 * If cntrl is added as a modifier then cells already selected are left selected, otherwise previous selections are
 * unselected before the new selection is done.
 * http://jsfiddle.net/0LLd8keu/2/ has an example of this code in action
 * @param tableName name of table to select cells from
 */
function tableSelect(tableName) {
    var tds = document.getElementById(tableName).getElementsByTagName('td');
    var selectionPivot;
    // 1 for left button, 2 for middle, and 3 for right.
    var LEFT_MOUSE_BUTTON = 1;
    var idTds = $(tds);
    idTds.each(function (idx, val) {
        // onselectstart because IE doesn't respect the css `user-select: none;`
        val.onselectstart = function () {
            return false;
        };
        // Prevent cntrl/click from displaying browser menu on table elements
        // Alternative is to have oncontextmenu="return false;" set on table body html tag
        $(idTds[idx]).on("contextmenu", function(evt) {evt.preventDefault();});
        // Now declare event handler for click
        $(val).mousedown(function (event) {
            if (event.which != LEFT_MOUSE_BUTTON) {
                return;
            }
            var ele = tds[idx];
            // Just a click - make current selection single cell clicked on
            if (!event.ctrlKey && !event.shiftKey) {
                clearAll();
                toggleElem(ele);
                selectionPivot = idx;
                return;
            }
            // Cntrl/Shift click - add to current selections cells between (inclusive) last single selection and cell
            // clicked on
            if (event.ctrlKey && event.shiftKey) {
                selectElemsBetweenIndexes(selectionPivot, idx);
                return;
            }
            // Cntrl click - keep current selections and add toggle of selection on the single cell clicked on
            if (event.ctrlKey) {
                toggleElem(ele);
                selectionPivot = idx;
            }
            // Shift click - make current selections cells between (inclusive) last single selection and cell clicked on
            if (event.shiftKey) {
                clearAll();
                selectElemsBetweenIndexes(selectionPivot, idx);
            }
        });
    });

    // Toggle element between selected and not selected
    function toggleElem(elem) {
        elem.className = elem.className == 'selected' ? '' : 'selected';
    }

    // Set elements in a range to all be selected
    function selectElemsBetweenIndexes(ia, ib) {
        var bot = Math.min(ia, ib);
        var top = Math.max(ia, ib);

        for (var i = bot; i <= top; i++) {
            tds[i].className = 'selected';
        }
    }

    // Set all elements to not be selected
    function clearAll() {
        for (var i = 0; i < tds.length; i++) {
            tds[i].className = '';
        }
    }
}
