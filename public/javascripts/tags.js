/**
 * Create tags and associate values
 * Created by nnovod on 4/21/15.
 */
function getTags(tagsTag, selectedValue, other, otherTagID) {
    $.ajax(jsRoutes.controllers.Application.tags())
        .done(function(data) {
            var ds = data.sort();
            var sel = ' selected="selected"';
            for (tag in ds) {
                var selT = "";
                if (selectedValue && selectedValue == ds[tag]) {
                    selT = sel;
                }
                $('<option value="' + ds[tag] + '"' + selT + '>' + ds[tag] + '</option>').appendTo('#' + tagsTag)
            }
            if (other) {
                var selO = "";
                if (selectedValue && selectedValue == "other...") {
                    selO = ' selected="selected"';
                    document.getElementById(otherTagID).type = 'text';
                }
                $('<option value="other..."' + selO + '>other...</option>').appendTo('#' + tagsTag);
                $('#' + tagsTag).on('change',function(){
                    onChangeToOther($(this).val(), otherTagID);
                });
            }
        })
        .fail(function(err) {
            $('<option value="' + err + '">' + err + '</option>').appendTo('#' + tagsTag)
        });
}

function onChangeToOther(val, hiddenTag) {
    if (val == 'other...') {
        document.getElementById(hiddenTag).type = 'text';
    }
    else document.getElementById(hiddenTag).type = 'hidden';
}

function makeTagDiv(post, addDiv, tagsID, tagsName, tagValue, otherTagID, otherTagName, other) {
    $(function() {
        var tagDL = '<dl id="' + tagsID + '_field">' +
        '<dt><label for="' + tagsID + '">Tag</label></dt>' +
        '<dd><select id="' + tagsID + '" name="' + tagsName + '"></select>' +
            '<input type="hidden" id="' + otherTagID +
            '" name="' + otherTagName + '" value="" placeholder="new tag"/>' +
        '<dd class="info">Required</dd></dl>';
        $('<div>' + tagDL + post + '</div>').appendTo($('#' + addDiv));
        getTags(tagsID, tagValue, other, otherTagID);
    });
}

function makeTagValue(ctName, ctValue, remTag, initValue) {
    return '<dl id="' + ctValue + '_field">' +
        '<dt><label for="' + ctValue + '">Value</label></dt>' +
        '<dd><textarea id="' + ctValue +
        '" name="' + ctName + '">' + initValue + '</textarea></dd>' +
        '</dl><a href="#" class="' + remTag + '">Remove Tag</a>';

}

function makeTags(inputDiv, componentTags, addTag, tagKey, valueKey, remTag, otherTag, other) {
    $(function() {
        $('#' + addTag).click(function () {
            var i = $('#' + inputDiv + ' div').size();
            var ctName = componentTags.replace(".", "_");
            var ctValue = ctName + '_' + i + '_' + valueKey
            var value = makeTagValue(componentTags + '[' + i + '].' + valueKey, ctValue, remTag, "")
            var tagsTag = ctName + "_" + i + "_tag";
            var hiddenTagID = ctName + "_" + i + "_" + otherTag;
            makeTagDiv(value, inputDiv, tagsTag, componentTags + '[' + i + '].' + tagKey, "",
                hiddenTagID, componentTags + '[' + i + '].' + otherTag, other);
            return false;
        });
        $(document).on('click', '.' + remTag, function () {
            $(this).parent('div').remove();
            return false;
        });
        return false;
    });
    return false;
}
