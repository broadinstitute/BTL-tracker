/**
 * Create tags and associate values
 * Created by nnovod on 4/21/15.
 */
function getTags(tagsTag, selectedValue) {
    $.ajax(jsRoutes.controllers.Application.tags())
        .done(function(data) {
            var ds = data.sort();
            for (tag in ds) {
                var sel = (selectedValue && selectedValue == ds[tag]) ? " selected=\"selected\"" : "";
                $('<option value="' + ds[tag] + sel + '">' + ds[tag] + '</option>').appendTo('#' + tagsTag)
            }
            $('<option value="other...">other...</option>').appendTo('#' + tagsTag);
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

function makeTags(inputDiv, componentTags, addTag, tagKey, valueKey, remTag, otherTag) {
    $(function() {
        var inpDiv = inputDiv;
        var addDiv = $('#' + inpDiv);
        var i = $('#' + inpDiv + ' div').size();
        $('#' + addTag).click(function () {
            var tagsTag = "tags_" + i + "_tag";
            var hiddenTagID = "tags_" + i + "_" + otherTag;
            var hiddenTagIDStr = "'" + hiddenTagID + "'";
            $('<div><dl id="tags_' + i + '_tag_field">' +
                '<dt><label for="' + tagsTag + '">Tag</label></dt>' +
                '<dd><select id=' + tagsTag + ' name="' + componentTags + '[' + i + '].' + tagKey +
                '" value="" onChange="onChangeToOther(this.value,' + hiddenTagIDStr + ')"></select>' +
                '<input type="hidden" id=' + hiddenTagID +
                ' name="' + componentTags + '[' + i + '].' + otherTag + '" value="" placeholder="new tag"/>' +
                '<dd class="info">Required</dd>' +
                '</dl>' + '<dl id="tags_' + i + '_value_field">' +
                '<dt><label for="tags_' + i + '_value">Value</label></dt>' +
                '<dd><textarea id="tags_' + i +
                '_value" name="' + componentTags + '[' + i + '].' + valueKey + '"></textarea></dd>' +
                '</dl><a href="#" class="' + remTag + '">Remove Tag</a></div>').appendTo(addDiv);
            getTags(tagsTag, "");
            i++;
            return false;
        });
        $(document).on('click', '.' + remTag, function () {
            $(this).parent('div').remove();
            return false;
        });
        return false;
    })
    return false;
}
