/**
 * Create tags and associated values
 * Created by nnovod on 4/21/15.
 */

/**
 * Async function to fill in a tag's select list.  We query the server to get the list of tags already in use and then
 * fill in the select's options with a sorted list of returned values.
 * @param tagsTag id of tag's select
 * @param selectedValue value chosen from select list
 * @param other true if new tags can be added (tags select list should include "other...")
 * @param otherTagID html id of hidden/text used to input new tag value
 */
function getTags(tagsTag, selectedValue, other, otherTagID) {
    $.ajax(jsRoutes.controllers.Application.tags())
        .done(function(data) {
            var ds = data.sort();
            var sel = ' selected="selected"';
            var ot = 'other...';
            // Put in all the options, selecting one that matches selected value
            for (tag in ds) {
                var selT = "";
                if (selectedValue && selectedValue == ds[tag]) {
                    selT = sel;
                }
                $('<option value="' + ds[tag] + '"' + selT + '>' + ds[tag] + '</option>').appendTo('#' + tagsTag)
            }
            // If new tag can be added add it as an option as well (with on change trigger to add text field to enter
            // new tag if other option chosen to add a new tag).
            if (other) {
                var selO = "";
                if (selectedValue && selectedValue == ot) {
                    selO = sel;
                    document.getElementById(otherTagID).type = 'text';
                }
                $('<option value="' + ot + '"' + selO + '>' + ot + '</option>').appendTo('#' + tagsTag);
                $('#' + tagsTag).on('change',function(){
                    onChangeToOther($(this).val(), otherTagID);
                });
            }
        })
        .fail(function(err) {
            $('<option value="' + err + '">' + err + '</option>').appendTo('#' + tagsTag)
        });
}

/**
 * Little guy to toggle box where new tags are input between hidden and text.  When the tag "other..." is chosen
 * the text box is made visible to allow the new tag to be entered.  Otherwise the text box is hidden.
 * @param val value set in select
 * @param hiddenTag id of hidden/text used to input new tag value
 */
function onChangeToOther(val, hiddenTag) {
    if (val == 'other...') {
        document.getElementById(hiddenTag).type = 'text';
    }
    else document.getElementById(hiddenTag).type = 'hidden';
}

/**
 * Function to make the entire div for a tag.  The associated value is usually included here as well as the "post"
 * input argument.  The values for the tag select list are filled in asynchronously via the getTags function.
 * @param post html to set within div after tag html
 * @param addDiv div to include all tags (each tag/value pair is set as its own div within the inputDiv
 * @param tagsID html id
 * @param tagsName html name
 * @param tagValue value for tag
 * @param otherTagID html id of hidden/text used to input new tag value
 * @param otherTagName html name of hidden/text used to input new tag value
 * @param other true if new tags can be added (tags select list should include "other...")
 */
function makeTagDiv(post, addDiv, tagsID, tagsName, tagValue, otherTagID, otherTagName, other) {
    $(function() {
        var tagDL = '<dl id="' + tagsID + '_field">' +
        '<dt><label for="' + tagsID + '">Tag</label></dt>' +
        '<dd><select id="' + tagsID + '" name="' + tagsName + '"></select>' +
            '<input type="hidden" id="' + otherTagID +
            '" name="' + otherTagName + '" value="" placeholder="new tag"/>' +
        '<dd class="info">Required</dd></dl>';
        // var divID = addDiv + "_" + divIndex;
        $('<div>' + tagDL + post + '</div>').appendTo($('#' + addDiv));
        getTags(tagsID, tagValue, other, otherTagID);
    });
}


/**
 * Function to make the html for the value textarea associated with a tag.
 * @param valName html name
 * @param valId html id
 * @param remTag html class for remove link
 * @param initValue initial value for textbox
 * @returns {string}
 */
function makeTagValue(valName, valId, remTag, initValue) {
    return '<dl id="' + valId + '_field">' +
        '<dt><label for="' + valId + '">Value</label></dt>' +
        '<dd><textarea id="' + valId +
        '" name="' + valName + '">' + initValue + '</textarea></dd>' +
        '</dl><a href="#" class="' + remTag + '">Remove Tag</a>';
}

/**
 * This function does not actually add/remove tags.  Instead it sets up on click functions to add (when addTag is
 * selected) or remove (when remTag is selected) tags.  The tags are differentiated from each other by a global
 * (within this function) index (i) that is put into the ids and names.  This index will only increase, as tags are
 * added, thus leaving gaps if tags are removed.  That's should not be a problem since the gaps are removed when the
 * array is put back together on the server side.
 * @param inputDiv div to include all tags (each tag/value pair is set as its own div within the inputDiv
 * @param componentTags prefix for all tag/value ids/names for html
 * @param addTag id of link to add a new tag/value
 * @param tagKey postfix of id/name for select used to choose tag
 * @param valueKey postfix of id/name for textbox used for value
 * @param remTag class to be used for link to remove a tag/value
 * @param otherTag postfix to be put on id/name for input of new tag
 * @param other true if new tags can be added (tags select list should include "other...")
 * @returns {boolean}
 */
function makeTags(inputDiv, componentTags, addTag, tagKey, valueKey, remTag, otherTag, other) {
    $(function() {
        // Get initial index past tags already there
        var i = $('#' + inputDiv + ' div').size();
        // ids use "_" inplace of "." in names
        var ctId = componentTags.replace(".", "_");
        // Add tag handler for link placed at end of existing tags
        $('#' + addTag).click(function () {
            // Best to get next number by having divisions have IDs with _x at end - then just use that id instead of
            // this mess which relies on div/dl/dd/select struction
            var xx = $('div > dl > dd > select', '#' + inputDiv);
            var xxx = 0;
            if (xx && xx.size()) {
                var xxs = xx.size();
                xx = xx[xxs-1].getAttribute("id");
                var re = /_(\d+)_/;
                var xx = re.exec(xx);
                var xxl = xx.length;
                xx = parseInt(xx[xxl-1]) + 1;
                // Look for _x_ to find last number used
            } else {
                xx = 0;
            }
            // Make sure we include all divs - can miss initial setting if redisplay after error
            var ii = $('#' + inputDiv + ' div').size();
            i = Math.max(ii, i);
            var ctValue = ctId + '_' + i + '_' + valueKey;
            var value = makeTagValue(componentTags + '[' + i + '].' + valueKey, ctValue, remTag, "");
            var tagsTag = ctId + "_" + i + "_" + tagKey;
            var hiddenTagID = ctId + "_" + i + "_" + otherTag;
            makeTagDiv(value, inputDiv, tagsTag, componentTags + '[' + i + '].' + tagKey, "",
                hiddenTagID, componentTags + '[' + i + '].' + otherTag, other);
            // Set index for next time
            i++;
            return false;
        });
        // Remove tag handler for link placed within division containing tag to be removed
        $(document).on('click', '.' + remTag, function () {
            $(this).parent('div').remove();
            return false;
        });
        return false;
    });
    return false;
}
