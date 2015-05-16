/**
 * Create tags and associate values
 * Created by nnovod on 4/21/15.
 */
function makeTags(inputDiv, componentTags, addTag, tagKey, valueKey, remTag) {
    $(function() {
        var inpDiv = inputDiv;
        var addDiv = $('#' + inpDiv);
        var i = $('#' + inpDiv + ' div').size();
        $('#' + addTag).click(function () {
            var tagsTag = "tags_" + i + "_tag"
            $('<div><dl id="tags_' + i + '_tag_field">' +
                '<dt><label for="tags_' + i + '_tag">Tag</label></dt>' +
                '<dd><select id=' + tagsTag + ' name="' + componentTags + '[' + i + '].' + tagKey + '" value=""/></select>' +
                '<dd class="info">Required</dd>' +
                '</dl>' + '<dl id="tags_' + i + '_value_field">' +
                '<dt><label for="tags_' + i + '_value">Value</label></dt>' +
                '<dd><textarea id="tags_' + i + '_value" name="' + componentTags + '[' + i + '].' + valueKey + '"></textarea></dd>' +
                '</dl><a href="#" class="' + remTag + '">Remove Tag</a></div>').appendTo(addDiv);
            $.ajax(jsRoutes.controllers.Application.tags())
                .done(function(data) {
                    var ds = data.sort();
                    for (tag in ds) {
                        $('<option value="' + data[tag] + '">' + data[tag] + '</option>').appendTo('#' + tagsTag)
                    }
                })
                .fail(function(err) {
                    $('<option value="' + err + '">' + err + '</option>').appendTo('#' + tagsTag)
                });
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
