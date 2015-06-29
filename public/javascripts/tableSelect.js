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

    // Set elements in a range - note that selection is flipped - we want to go down rows
    function selectElemsBetweenIndexes(ia, ib) {
        // Get re: index row and element from original index
        function re(idx) {return {row: Math.floor(idx/elesPerRow), ele: idx%elesPerRow}}
        // Get index from re made from original index
        function getIdx(rei) {return rei.row*elesPerRow + rei.ele}
        // Get new re by flipping row and element
        function flipCoords(rei) {return {row: rei.ele, ele: rei.row}}
        // Get re from flipped index
        function flipRe(idx) {return {row: Math.floor(idx/numRows), ele: idx%numRows}}
        // Get index from flipped re
        function flipGetIdx(rei) {return rei.row*numRows + rei.ele}
        // Flip coordinates and then get flipped indexes
        var iaFlipRe = flipCoords(re(ia));
        var ibFlipRe = flipCoords(re(ib));
        var iaFlipIdx = flipGetIdx(iaFlipRe);
        var ibFlipIdx = flipGetIdx(ibFlipRe);
        // Find first and last index being selected
        var bot = Math.min(iaFlipIdx, ibFlipIdx);
        var top = Math.max(iaFlipIdx, ibFlipIdx);
        // Go through and mark selected elements - note we flip back to original coordinates to select element
        for (var i = bot; i <= top; i++) {
            var iRe = flipRe(i);
            var j = getIdx(flipCoords(iRe));
            tds[j].className = 'selected';
        }
    }

    // Set elements in a range - note that selection is flipped - we want to go down rows
    function selectElemsBetweenIndexes(ia, ib) {
        // Get index row and element
        function re(idx) {return {row: Math.floor(idx/elesPerRow), ele: idx%elesPerRow}}
        // Get row/element for two elements
        var iaPos = re(ia);
        var ibPos = re(ib);
        // Figure out "bottom" and "top" selection row/element
        var botEle = Math.min(iaPos.ele, ibPos.ele);
        var topEle = Math.max(iaPos.ele, ibPos.ele);
        var botRow, topRow;
        if (iaPos.ele == ibPos.ele) {
            botRow = Math.min(iaPos.row, ibPos.row);
            topRow = Math.max(iaPos.row, ibPos.row);
        } else if (iaPos.ele < ibPos.ele) {
            botRow = iaPos.row;
            topRow = ibPos.row;
        } else {
            botRow = ibPos.row;
            topRow = iaPos.row;
        }
        // Now go through all elements and select those in proper row/element
        for (var i = 0; i < tds.length; i++) {
            // Get row/element we're looking at
            iPos = re(i);
            // If only one element then just make sure one we're looking at is in right row
            if (botEle == topEle) {
                if (iPos.ele == botEle && iPos.row >= botRow && iPos.row <= topRow) {
                    tds[i].className = 'selected';
                }
            }
            // Otherwise it must be an element between ones we're looking at or row positioned correctly in bottom
            // or top element
            else if ((iPos.ele > botEle && iPos.ele < topEle) ||
                (iPos.ele == botEle && iPos.row >= botRow) || (iPos.ele == topEle && iPos.row <= topRow)) {
                tds[i].className = 'selected';
            }
        }
    }

    // Set all elements to not be selected
    function clearAll() {
        for (var i = 0; i < tds.length; i++) {
            tds[i].className = '';
        }
    }
}