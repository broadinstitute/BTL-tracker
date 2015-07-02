/**
 * Created by nnovod on 6/24/15.
 * Select cells from a table
 */
/**
 * Function to select cells from a table.  A click on a cell selects just that single cell if no click modifiers are
 * present, selects from the last single cell (un)selected to the newly selected cell if a shift/click.
 * If cntrl is added as a modifier then cells already selected are left selected, otherwise previous selections are
 * unselected before the new selection is done.
 * http://jsfiddle.net/0LLd8keu/9/ has an example of this code in action
 * @param tableName name of table to select cells from
 */
function tableSelect(tableName) {
    // Get array of html tds and total # of tds
    var tds = document.getElementById(tableName).getElementsByTagName('td');
    var numEles = tds.length;
    // Get array of html trs and total # of trs excluding rows that don't contain tds (i.e., header rows with just ths)
    var trs = document.getElementById(tableName).getElementsByTagName('tr');
    var numRows = 0;
    for (var i = 0; i < trs.length; i++) {
        var trTds = trs[i].getElementsByTagName('td');
        if (trTds.length > 0) {
            numRows++;
        }
    }
    // Get # of td elements per row
    var elesPerRow = numEles/numRows;
    // Last cell selected
    var selectionPivot = 0;
    // 1 for left button, 2 for middle, and 3 for right.
    var LEFT_MOUSE_BUTTON = 1;
    // Get jQuery array to loop through it
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
            // If not left mouse button then leave it for someone else
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

    // Set elements in a range - selection is flipped since we want to go down rows
    function selectElemsBetweenIndexes(ia, ib) {
        // Get re: index row and element from original index
        function re(idx) {return {row: Math.floor(idx/elesPerRow), ele: idx%elesPerRow}}
        // Get index from re made from original index
        function getIdx(rei) {return rei.row*elesPerRow + rei.ele}
        // Get new re by swapping row and element
        function swapCoords(rei) {return {row: rei.ele, ele: rei.row}}
        // Get re from flipped index
        function flipRe(idx) {return {row: Math.floor(idx/numRows), ele: idx%numRows}}
        // Get index from flipped re
        function flipGetIdx(rei) {return rei.row*numRows + rei.ele}
        // Flip indexes
        var iaFlipIdx = flipGetIdx(swapCoords(re(ia)));
        var ibFlipIdx = flipGetIdx(swapCoords(re(ib)));
        // Find first and last index being selected
        var bot = Math.min(iaFlipIdx, ibFlipIdx);
        var top = Math.max(iaFlipIdx, ibFlipIdx);
        // Go through and mark selected elements - note we flip back to original coordinates to select element
        for (var i = bot; i <= top; i++) {
            var j = getIdx(swapCoords(flipRe(i)));
            tds[j].className = 'selected';
        }
    }
    // Set all elements to not be selected
    function clearAll() {
        for (var i = 0; i < tds.length; i++) {
            tds[i].className = '';
        }
    }
}

/**
 * Retrieve indicies of selected table detail element.
 * @param tableName name of table
 * @returns {Array} contains integer indicies of selected detail elements (numbers are consecutive within a row)
 */
function findSelect(tableName) {
    // Get array of html tds and total # of tds
    var tds = document.getElementById(tableName).getElementsByTagName('td');
    // Get number of elements
    var numEles = tds.length;
    // Init array to return selected indicies
    var selectedEles = [];
    // Loop through saving indicies in array
    for (var i = 0; i < numEles; i++) {
        if (tds[i].className == 'selected') {
            selectedEles.push(i);
        }
    }
    return selectedEles;
}
