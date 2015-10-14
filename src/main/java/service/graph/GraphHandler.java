package service.graph;

import config.AppConfig;
import model.FriendInfo;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.graph.DegreeRangeBuilder.DegreeRangeFilter;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.preview.PNGExporter;
import org.gephi.io.exporter.preview.SVGExporter;
import org.gephi.preview.api.*;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.ranking.api.Ranking;
import org.gephi.ranking.api.RankingController;
import org.gephi.ranking.api.Transformer;
import org.gephi.ranking.plugin.transformer.AbstractSizeTransformer;
import org.gephi.statistics.plugin.GraphDistance;
import org.gephi.statistics.plugin.Modularity;
import org.gephi.utils.PaletteUtils;
import org.openide.util.Lookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import processing.core.PApplet;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This demo shows several actions done with the toolkit, aiming to do a complete chain,
 * from data import to results.
 * <p/>
 * This demo shows the following steps:
 * <ul><li>Create a project and a workspace, it is mandatory.</li>
 * <li>Import the <code>polblogs.gml</code> graph file in an import container.</li>
 * <li>Append the container to the main graph structure.</li>
 * <li>Filter the graph, using <code>DegreeFilter</code>.</li>
 * <li>Run layout manually.</li>
 * <li>Compute graph distance metrics.</li>
 * <li>Rank color by degree values.</li>
 * <li>Rank size by centrality values.</li>
 * <li>Configure preview to display labels and mutual edges differently.</li>
 * <li>Export graph as PDF.</li></ul>
 *
 * @author Mathieu Bastian
 */
public class GraphHandler {
    private static final Logger log = LoggerFactory.getLogger(GraphHandler.class);

    private Map<FriendInfo, List<FriendInfo>> dataMap;

    int minSize = 10;
    int maxSize = 50;

    public GraphHandler(Map<FriendInfo, List<FriendInfo>> dataMap) {
        this.dataMap = dataMap;
    }

    private float rgbInt2Float(int a) {
        return a / 256f;
    }

    public void script() throws IOException {
        log.info("Begin to draw");
        //Init a project - and therefore a workspace
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        Workspace workspace = pc.getCurrentWorkspace();

        // Get models and controllers for this new workspace - will be useful later
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
        PreviewController previewController = Lookup.getDefault().lookup(PreviewController.class);
        PreviewModel model = previewController.getModel();
        FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
        RankingController rankingController = Lookup.getDefault().lookup(RankingController.class);

        // Import the data
        DirectedGraph directedGraph = graphModel.getDirectedGraph();
        Map<FriendInfo, Node> nodes = new HashMap<>();

        for (Map.Entry<FriendInfo, List<FriendInfo>> entry : dataMap.entrySet()) {
            FriendInfo friendInfo = entry.getKey();
            Node n = graphModel.factory().newNode(friendInfo.getUid());
            n.getNodeData().setLabel(friendInfo.getName());
            nodes.put(friendInfo, n);
            directedGraph.addNode(n);
        }

        for (Map.Entry<FriendInfo, List<FriendInfo>> entry : dataMap.entrySet()) {
            FriendInfo key = entry.getKey();
            for (FriendInfo ele : entry.getValue()) {
                if (!nodes.containsKey(ele)) continue; // 容错
                Edge edge = graphModel.factory().newEdge(nodes.get(key), nodes.get(ele), 1f, true);
                directedGraph.addEdge(edge);
            }
        }

        // Filter
        DegreeRangeFilter degreeFilter = new DegreeRangeFilter();
        degreeFilter.init(directedGraph);
        degreeFilter.setRange(new Range(1, Integer.MAX_VALUE)); //Remove nodes with degree < 30
        Query query = filterController.createQuery(degreeFilter);
        GraphView view = filterController.filter(query);
        graphModel.setVisibleView(view); //Set the filter result as the visible view

        // See visible graph stats
        UndirectedGraph graphVisible = graphModel.getUndirectedGraphVisible();
        log.info("---The Graph Info---");
        log.info("Nodes: " + graphVisible.getNodeCount());
        log.info("Edges: " + graphVisible.getEdgeCount());

        // Get Centrality
        GraphDistance distance = new GraphDistance();
        distance.setDirected(true);
        distance.execute(graphModel, attributeModel);

        // 分配节点大小
        AttributeColumn centralityColumn = attributeModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
        Ranking centralityRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, centralityColumn.getId());
        AbstractSizeTransformer sizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_SIZE);
        sizeTransformer.setMinSize(minSize);
        sizeTransformer.setMaxSize(maxSize);
        rankingController.transform(centralityRanking, sizeTransformer);

        // 根据 modularity 分配节点颜色
        Modularity modularity = new Modularity();
        modularity.execute(graphModel, attributeModel);

        String input = modularity.getReport();
        String regex = "\\bNumber of Communities\\b\\:\\s(\\d)\\b<br";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        int colorCount = 27;
        if (matcher.find()) {
            colorCount = Integer.parseInt(matcher.group(1));
        }
        AttributeColumn modularityColumn = attributeModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
        Color[] colors = PaletteUtils.getSequenceColors(colorCount).toArray(new Color[colorCount]);
        for (Node node : graphModel.getDirectedGraph().getNodes()) {
            int rgb = (int) node.getNodeData().getAttributes().getValue(modularityColumn.getIndex());
            Color colorBuf = colors[rgb % colorCount];
            node.getNodeData().setColor(rgbInt2Float(colorBuf.getRed()), rgbInt2Float(colorBuf.getGreen()), rgbInt2Float(colorBuf.getBlue()));
        }

        // 根据策略执行布局算法
        log.info("Running layout algorithm...");
        LayOutStrategyFactory.runLayoutAlgorithm(AppConfig.LAYOUT_ALOG_ENUM, graphModel);
        log.info("Layout end");

        // Preview
        model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, AppConfig.SHOW_NODE_LABELS);
        model.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.GRAY));
        model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, new Font("宋体", Font.PLAIN, 10));
        model.getProperties().putValue(PreviewProperty.EDGE_CURVED, false);
        previewController.refreshPreview();

        // New Processing target, get the PApplet
        ProcessingTarget target = (ProcessingTarget) previewController.getRenderTarget(RenderTarget.PROCESSING_TARGET);
        PApplet applet = target.getApplet();
        applet.init();

        // Refresh the preview and reset the zoom
        previewController.render(target);
        target.refresh();
        target.resetZoom();

        // Add the applet to a JFrame and display
        log.info("Build JFrame");
        JFrame frame = new JFrame("Renren Friend Relationship");
        frame.setLayout(new BorderLayout());

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(applet, BorderLayout.CENTER);

        frame.pack();
        frame.setVisible(true);
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        PNGExporter pe = new PNGExporter();
        ec.exportFile(new File("headless_simple.png"), pe);
        SVGExporter ps = new SVGExporter();
        ec.exportFile(new File("headless_simple.svg"), ps);
    }

}
