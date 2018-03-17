package viewer.layers.aslan;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import viewer.K_ScreenTransform;
import viewer.K_ViewerLayer;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class A_AreasRoadDetectorScore extends K_ViewerLayer {

    @Override
    public void paint(Graphics2D g2, K_ScreenTransform kst, AURWorldGraph wsg, AURAreaGraph selected_ag) {
        g2.setStroke(new BasicStroke(2));
        g2.setFont(new Font("Arial", 0, 9));
        g2.setColor(Color.BLACK);
        
        for(AURAreaGraph ag : wsg.areas.values()) {
            String score = String.valueOf(ag.getFinalScore());
            g2.drawString(score, kst.xToScreen(ag.getX()),  kst.yToScreen(ag.getY()));
        }
        g2.setStroke(new BasicStroke(1));
    }
}
