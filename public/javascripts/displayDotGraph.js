/**
 * Convert a dot graph into a svg document which is displayed in the window
 * Created by nnovod on 4/21/15.
 */
function displayDot(graph)
{
    window.onload = function () {
        // Using Viz library - uses dot attributes that graphlib library ignores
        // Get html with svg and insert it into the document
        var svg = Viz(graph, format = "svg", engine = "dot");
        $("#graphContainer").append(svg);
        // Fix up size of window and visible height/width
        var svgDoc = document.querySelector('#graphContainer');
        var bbox = svgDoc.getBBox();
        window.resizeTo(bbox.width + 60, bbox.height + 120);
        document.getElementById("graphContainer").style.height = bbox.height;
        document.getElementById("graphContainer").style.width = bbox.width;

        // Using graplib and d3 libraries...
        // Parse the DOT syntax into a graphlib object.
        //var g = graphlibDot.read(graph);
        // Render the graphlib object using d3.
        // Create and configure the renderer
        //var render = new dagreD3.render();
        // Set up an SVG group so that we can translate the final graph.
        //var svg = d3.select("svg"),
        //    inner = svg.append("g");
        // Run the renderer. This is what draws the final graph.
        //render(inner, g);
        // Center the graph - first, if graph bigger than svg (plus margins) then enlarge svg
        //if (svg.attr("width") <= (g.graph().width + 40)) {
        //    svg.attr("width", g.graph().width + 40)
        //}
        //var xCenterOffset = (svg.attr("width") - g.graph().width) / 2;
        //inner.attr("transform", "translate(" + xCenterOffset + ", 20)");
        //svg.attr("height", g.graph().height + 40);    // Resize the window height as well
        // Resize the window based on the contents.
        //var svgDoc = document.querySelector('#graphContainer');
        //var bbox = svgDoc.getBBox();
        //window.resizeTo(bbox.width + 60, bbox.height + 120);

        return false;
    }
}
