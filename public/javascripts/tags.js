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
            for (var tag in ds) {
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
                // If new tag is selected option or the tags list is empty except for the option to add a new tag then
                // make new tag selected one and make associated text field visible
                if ((selectedValue && selectedValue == ot) || ds.length == 0) {
                    selO = sel;
                    document.getElementById(otherTagID).type = 'text';
                }
                $('<option value="' + ot + '"' + selO + '>' + ot + '</option>').appendTo('#' + tagsTag);
                $('#' + tagsTag).on('change',function(){
                    onChangeToOther($(this).val(), otherTagID, ot);
                });
            }
        })
        .fail(function(err) {
            $('<option value="">' + err.responseText + '</option>').appendTo('#' + tagsTag)
        });
}

/**
 * Little guy to toggle box where new tags are input between hidden and text.  When the tag "other..." is chosen
 * the text box is made visible to allow the new tag to be entered.  Otherwise the text box is hidden.
 * @param val value set in select
 * @param hiddenTag id of hidden/text used to input new tag value
 * @param otherVal string constant for tag to pick "other..." to specify a new tag
 */
function onChangeToOther(val, hiddenTag, otherVal) {
    if (val == otherVal) {
        document.getElementById(hiddenTag).type = 'text';
    }
    else document.getElementById(hiddenTag).type = 'hidden';
}

/**
 * Function to make the entire div for a tag.  The associated value is usually included here as well as the "post"
 * input argument.  The values for the tag select list are filled in asynchronously via the getTags function.
 * @param divIndex integer index for the div being created inside the addDiv div
 * @param post html to set within div after tag html
 * @param addDiv id of div to include all tags (each tag/value pair is set as its own div within addDiv)
 * @param tagsID html id
 * @param tagsName html name
 * @param tagValue value for tag
 * @param otherTagID html id of hidden/text used to input new tag value
 * @param otherTagName html name of hidden/text used to input new tag value
 * @param otherValue value for new tag associated with other choice
 * @param other true if new tags can be added (tags select list should include "other...")
 */
function makeTagDiv(divIndex, post, addDiv, tagsID, tagsName, tagValue, otherTagID, otherTagName, otherValue, other) {
    $(function() {
        var tagDL = '<dl id="' + tagsID + '_field">' +
        '<dt><label for="' + tagsID + '">Tag</label></dt>' +
        '<dd><select id="' + tagsID + '" name="' + tagsName + '"></select>' +
            '<input type="hidden" id="' + otherTagID +
            '" name="' + otherTagName + '" value="' + otherValue + '" placeholder="new tag"/>' +
        '<dd class="info">Required</dd></dl>';
        var divID = addDiv + "_" + divIndex;
        $('<div id="' + divID + '">' + tagDL + post + '</div>').appendTo($('#' + addDiv));
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
 * selected) or remove (when remTag is selected) tags.  The tags are differentiated from each other by an index
 * computed within this function) that is put into the ids and names.  This index will only increase, as tags are
 * added, thus leaving gaps if tags are removed.  That should not be a problem since the gaps are removed when the
 * array is put back together on the server side.
 * @param inputDiv div to include all tags (each tag/value pair is set as its own div within the inputDiv
 * @param tagPrefix prefix for all tag/value ids/names for html
 * @param addTag id of link to add a new tag/value
 * @param tagKey postfix of id/name for select used to choose tag
 * @param valueKey postfix of id/name for textbox used for value
 * @param remTag class to be used for link to remove a tag/value
 * @param otherTag postfix to be put on id/name for input of new tag
 * @param other true if new tags can be added (tags select list should include "other...")
 * @returns {boolean}
 */
function makeTags(inputDiv, tagPrefix, addTag, tagKey, valueKey, remTag, otherTag, other) {
    $(function() {
        // ids use "_" inplace of "." in names
        var idPrefix = tagPrefix.replace(".", "_");
        // Add tag handler for link placed at end of existing tags
        $('#' + addTag).click(function () {
            // Get next number looking for highest number division used so far.  In makeTagDiv the tag div id is set to
            // inputDiv_x where x is an integer.  We now scan all the divs looking for the maximum number used so far.
            // We need to go through this process so we don't have to worry about tags deleted or forms redisplayed
            // after an error (in that case the divisions aren't remade so we can't keep a count).
            var tagDivs = $('div', '#' + inputDiv);
            var i = 0;
            // Set our index to largest used so far + 1
            if (tagDivs && tagDivs.size()) {
                var lastDivId = tagDivs[tagDivs.size()-1].getAttribute("id");
                var re = /(\d+)$/.exec(lastDivId);
                i = parseInt(re[re.length-1]) + 1;
            }
            // Put in a new tag div
            var iIdPrefix = idPrefix + '_' + i + '_';
            var iNamePrefix = tagPrefix + '[' + i + '].';
            var value = makeTagValue(iNamePrefix + valueKey, iIdPrefix + valueKey, remTag, "");
            makeTagDiv(i, value, inputDiv, iIdPrefix + tagKey, iNamePrefix + tagKey, "",
                iIdPrefix + otherTag, iNamePrefix + otherTag, "", other);
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
