package service.graph;

import org.gephi.graph.api.GraphModel;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingold;
import org.gephi.layout.plugin.labelAdjust.LabelAdjust;
import org.gephi.layout.plugin.multilevel.MaximalMatchingCoarsening;
import org.gephi.layout.plugin.multilevel.MultiLevelLayout;
import org.gephi.layout.plugin.random.RandomLayout;
import org.gephi.layout.plugin.rotate.RotateLayout;
import org.gephi.layout.plugin.scale.ScaleLayout;
import org.gephi.layout.spi.Layout;

public class LayOutStrategyFactory {

    // 执行布局算法
    public static void runLayoutAlgorithm(LayoutAlogEnum algoEnum, GraphModel graphModel) {
        Layout layout;
        int maxAlgoRunTimes = 1;

        switch (algoEnum) {
            case ForceAtlas:
                layout = new ForceAtlasLayout(null);
                maxAlgoRunTimes = 1500;
                break;
            case FruchtermanReingold:
                layout = new FruchtermanReingold(null);
                maxAlgoRunTimes = 3500;
                break;
            case LabelAdjust:
                layout = new LabelAdjust(null);
                maxAlgoRunTimes = 1000;
                break;
            case MultiLevel:
                layout = new MultiLevelLayout(null, new MaximalMatchingCoarsening());
                maxAlgoRunTimes = 10000;
                break;
            case Rotate:
                layout = new RotateLayout(null, 25.0); // not good
                maxAlgoRunTimes = 1000;
                break;
            case Random:
                layout = new RandomLayout(null, 1.0); // not good
                break;
            case Scale:
                layout = new ScaleLayout(null, 28);
                break;
            case YifanHu: default:
                layout = new YifanHuLayout(null, new StepDisplacement(1f));
                ((YifanHuLayout) layout).setOptimalDistance(200f);
                maxAlgoRunTimes = 1000;
                break;
        }
        layout.setGraphModel(graphModel);
        layout.resetPropertiesValues();
        layout.initAlgo();

        int stepCount = 1;
        for (; stepCount <= maxAlgoRunTimes && layout.canAlgo(); stepCount++) {
            layout.goAlgo();
        }
        System.out.println("DEBUG: The last count: " + stepCount + " ");
        layout.endAlgo();
    }

}
