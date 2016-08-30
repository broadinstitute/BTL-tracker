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
    // Get # of td elements per row and per column
    var elesPerRow = numEles/numRows;
    // Last min. cell selected
    var selectionMinPivot = 0;
    // Last max. cell selected
    var selectionMaxPivot = 0;
    // 1 for left button, 2 for middle, and 3 for right.
    var LEFT_MOUSE_BUTTON = 1;
    // Get jQuery array to loop through it
    var idTds = $(tds);
    // Get array of html ths and total # of ths - need to use Array.prototype since getElementsByTagName returns
    // a NodeList which does not support array functions (e.g., filter) and we want to filter out any elements
    // with no inner text (e.g., the corner th which is not a row/column header)
    var ths = Array.prototype.filter.call(document.getElementById('cherryTable').getElementsByTagName('th'),
        function(x) {return x.innerText != '' });
    // Set # of headers that are for columns (same as # of elements per row)
    var colHeaders = elesPerRow;
    // Setup event handler for headers - first get jQuery array to loop through it
    var idThs = $(ths);
    // Setup event handler for headers
    idThs.each(function (idx, val) {
        // Now declare event handler for click
        $(val).mousedown(function (event) {
            initElem(val);
            // Is it a column header?
            var colHeader = idx < colHeaders;
            // Get index of first element to be selected
            var eleIdx = colHeader ? idx : (idx - colHeaders) * elesPerRow;
            // Init array to return selected indicies
            var selectedEles = [];
            // Loop through saving indicies in array
            if (colHeader)  {
                for (var i = eleIdx; i < numEles; i = i + elesPerRow) {
                    selectedEles.push(i);
                }
            } else {
                for (var ii = eleIdx; ii < eleIdx + elesPerRow; ii++) {
                    selectedEles.push(ii);
                }
            }
            doSelections(event, eleIdx, selectedEles, colHeader);
        });
    });
    // Setup event handler for details
    idTds.each(function (idx, val) {
        // Now declare event handler for click
        $(val).mousedown(function (event) {
            initElem(val);
            doSelections(event, idx, [idx], true);
        });
    });

    // Do selections with
    // Event that happened
    // Index to first element selected
    // Array of indicies selected (in sorted order)
    // Columns being selected
    function doSelections(event, eleIdx, selectedEles, cols) {
        // Select elements in a range, either across rows or columns
        function selectRange() {
            var frstSelect = selectedEles[0];
            var lstSelect = selectedEles[selectedEles.length - 1];
            if (cols) {
                var start = flipFlipIdx(Math.min(flipIdx(selectionMinPivot), flipIdx(selectionMaxPivot),
                    flipIdx(frstSelect), flipIdx(lstSelect)));
                var end = flipFlipIdx(Math.max(flipIdx(selectionMinPivot), flipIdx(selectionMaxPivot),
                    flipIdx(frstSelect), flipIdx(lstSelect)));
                selectElemsDownRows(Math.min(start, end), Math.max(start, end));
            } else {
                var start = Math.min(selectionMinPivot, frstSelect);
                var end = Math.max(selectionMaxPivot, lstSelect);
                selectElemRange(start, end);
            }
        }
        // If not left mouse button then leave it for someone else
        if (event.which != LEFT_MOUSE_BUTTON) {
            return;
        }
        // Just a click - make selection current row/column clicked on
        if (!event.ctrlKey && !event.shiftKey) {
            clearAll();
            selectEles(selectedEles);
            selectionMinPivot = selectedEles[0];
            selectionMaxPivot = selectedEles[selectedEles.length - 1];
            return;
        }
        // Cntrl/Shift click - add to current selections cells between (inclusive) last single selection and cell
        // clicked on
        if (event.ctrlKey && event.shiftKey) {
            selectRange();
            return;
        }
        // Cntrl click - keep current selections and add toggle of selection on the single cell clicked on
        if (event.ctrlKey) {
            toggleEles(selectedEles);
            selectionMinPivot = selectedEles[0];
            selectionMaxPivot = selectedEles[selectedEles.length - 1];
            return;
        }
        // Shift click - make current selections cells between (inclusive) last single selection and cell clicked on
        if (event.shiftKey) {
            clearAll();
            selectRange();
        }
    }

    // Setup some initial behavior when there's a mouse click
    function initElem(val) {
        // onselectstart because IE doesn't respect the css `user-select: none;`
        val.onselectstart = function () {
            return false;
        };
        // Prevent cntrl/click from displaying browser menu on table elements
        // Alternative is to have oncontextmenu="return false;" set on table body html tag
        $(val).on("contextmenu", function(evt) {evt.preventDefault();});
    }

    // Select elements using array of indicies
    function selectEles(a) {
        for (var i = 0; i < a.length; i++) {
            tds[a[i]].className = 'selected';
        }
    }

    // Toggle elements using array of indicies
    function toggleEles(a) {
        for (var i = 0; i < a.length; i++) {
            toggleElem(tds[a[i]]);
        }
    }

    // Toggle element between selected and not selected
    function toggleElem(elem) {
        elem.className = elem.className == 'selected' ? '' : 'selected';
    }

    function selectElemRange(ia, ib) {
        var bot = Math.min(ia, ib);
        var top = Math.max(ia, ib);
        for (var i = bot; i <= top; i++) {
            tds[i].className = 'selected';
        }
    }

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
    // Get flipped index
    function flipIdx(idx) {return flipGetIdx(swapCoords(re(idx)))}
    // Get flipped index flipped back
    function flipFlipIdx(idx) {return getIdx(swapCoords(flipRe(idx)))}
    // Set elements in a range - selection is flipped since we want to go down rows
    function selectElemsDownRows(ia, ib) {
        // Flip indexes
        var iaFlipIdx = flipIdx(ia);
        var ibFlipIdx = flipIdx(ib);
        // Find first and last index being selected
        var bot = Math.min(iaFlipIdx, ibFlipIdx);
        var top = Math.max(iaFlipIdx, ibFlipIdx);
        // Go through and mark selected elements - note we flip back to original coordinates to select element
        for (var i = bot; i <= top; i++) {
            var j = flipFlipIdx(i);
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

/**
 * Upon form submission set form values from cherry picking table.
 * @param formName name of form to be submitted
 * @param tableName name of cherry picking table
 * @param cherriesKey keyword to use for form array to be set with picked wells.
 */
function submitSelect(formName, tableName, cherriesKey) {
    $('#' + formName).submit( function(eventObj){
        var sel = findSelect(tableName);
        for (var i = 0; i < sel.length; i++) {
            var idName = cherriesKey + '[' + i + ']';
            $(this).append('<input type="hidden" id="' + idName + '" name="' + idName + '" value="' + sel[i] + '"/>');
        }
        return true;
    })
}
