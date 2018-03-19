package AUR.util.aslan;

import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURConstants;
import AUR.util.knd.AURWorldGraph;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.AbstractModule;
import adf.component.module.algorithm.Clustering;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Amir Aslan Aslani - Mar 2018
 */
public class AURPoliceScoreGraph extends AbstractModule {
        public HashMap<EntityID, AURAreaGraph> areas = new HashMap<>();
        private Clustering clustering;
        public AgentInfo ai;
        public WorldInfo wi;
        public ScenarioInfo si;
        public AURWorldGraph wsg;
        
        public PriorityQueue<AURAreaGraph> pQueue = new PriorityQueue<>(new AURPoliceAreaScoreComparator());
        
        public Collection<EntityID> clusterEntityIDs;
        int cluseterIndex;
        double myClusterCenter[] = new double[2];
        StandardEntity myClusterCenterEntity = null;
        double maxDistToCluster = 0;
        
        double maxDisFromAgentStartPoint = 0;
        
        public AURPoliceScoreGraph(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
                super(ai, wi, si, moduleManager, developData);
                this.ai = ai;
                this.si = si;
                this.wi = wi;
                
                this.wsg = moduleManager.getModule("knd.AuraWorldGraph");
                
                this.clustering = moduleManager.getModule("SampleRoadDetector.Clustering", "AUR.module.algorithm.AURBuildingClusterer");
                this.cluseterIndex = this.clustering.calc().getClusterIndex(ai.me());
                this.clusterEntityIDs = this.clustering.getClusterEntityIDs(cluseterIndex);
                
                fillLists();
                setScores();
        }

        private void fillLists(){
                this.areas = wsg.areas;
                
                // ---
                
                int counter = 0;
                for(StandardEntity se : this.clustering.getClusterEntities(cluseterIndex)){
                        if(se instanceof Area){
                                myClusterCenter[0] += ((Area) se).getX();
                                myClusterCenter[1] += ((Area) se).getY();
                                counter ++;
                        }
                }
                myClusterCenter[0] /= counter;
                myClusterCenter[1] /= counter;
                double dis = Double.MAX_VALUE;
                
                for(StandardEntity se : this.clustering.getClusterEntities(cluseterIndex)){
                        if(se instanceof Area && Math.hypot(myClusterCenter[0] - ((Area) se).getX(), myClusterCenter[1] - ((Area) se).getY()) < dis){
                                dis = Math.hypot(myClusterCenter[0] - ((Area) se).getX(), myClusterCenter[1] - ((Area) se).getY());
                                myClusterCenterEntity = se;
                        }
                }
                myClusterCenter[0] = ((Area) myClusterCenterEntity).getX();
                myClusterCenter[1] = ((Area) myClusterCenterEntity).getY();
                
                // ---
                
                for(AURAreaGraph entity : wsg.areas.values()){
                        if( Math.hypot(entity.getX() - myClusterCenter[0], entity.getY() - myClusterCenter[1]) > maxDistToCluster){
                                maxDistToCluster = Math.hypot(entity.getX() - myClusterCenter[0], entity.getY() - myClusterCenter[1]);
                        }
                }
                maxDistToCluster += 50;
                // --
                
                Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> worldBounds = wi.getWorldBounds();
                
                int[][] bound = new int[][]{
                        {worldBounds.first().first(),worldBounds.first().second()},
                        {worldBounds.second().first(),worldBounds.second().second()},
                        {worldBounds.first().first(),worldBounds.second().second()},
                        {worldBounds.second().first(),worldBounds.first().second()}
                };
                for(int[] point : bound){
                        if(Math.hypot(point[0] - agentInfo.getX(), point[1] - agentInfo.getY()) > maxDisFromAgentStartPoint)
                                maxDisFromAgentStartPoint = Math.hypot(point[0] - agentInfo.getX(), point[1] - agentInfo.getY());
                }
        }
        
        @Override
        public AbstractModule calc() {
                return this;
        }

        @Override
        public AbstractModule updateInfo(MessageManager messageManager) {
                pQueue.clear();
                
                // Set dynamic scores
                wsg.NoBlockadeDijkstra(ai.getPosition());
                decreasePoliceAreasScore(0.8);
                
                for(AURAreaGraph area : wsg.areas.values()){
                        setDistanceScore(area, 0.1);
                        setAliveBlockadesScore(area, 0.0);
                        blockedHumansScore(area, 1.2);
                }
                pQueue.addAll(wsg.areas.values());
                return this;
        }

        private void setScores() {
                wsg.NoBlockadeDijkstra(ai.getPosition());
                
                addAreasConflictScore(0.02);
                
                for(AURAreaGraph area : wsg.areas.values()){
                        setDistanceScore(area, 0.1);
                        addRefugeScore(area, 0.15);
                        addGasStationScore(area, 0.075);
                        addHydrandScore(area, 0.05);
                        addWSGRoadScores(area, 0.075);
                        addClusterScore(area, 0.4);
                        
//                        blockadeExistancePossibilityScore(area, 0.075);
//                        humansBlockedPossibilityScore(area, 0.05);
//                        
                        pQueue.add(area);
                }
                
        }
        
        private void addAreasConflictScore(double score) {
                
        }

        private void addScoreToCollection(ArrayList<EntityID> collection, double score) {
                for(EntityID eid : collection){
                        areas.get(eid).baseScore += score;
                }
        }

        private void addWSGRoadScores(AURAreaGraph area, double score) {
                area.baseScore += area.getScore() * score;
        }

        private void addRefugeScore(AURAreaGraph area, double score) {
                if(! area.isRefuge()){
                        score = 0;
                }
                area.baseScore += score;
        }

        private void blockadeExistancePossibilityScore(AURAreaGraph area, double score) {
                
        }

        private void humansBlockedPossibilityScore(AURAreaGraph area, double d) {
                
        }

        private void addClusterScore(AURAreaGraph area, double score) {
                if(clusterEntityIDs.contains(area.area.getID())){
                        
                }
                else{
                        
                        double distanceFromCluster = Math.hypot(area.getX() - myClusterCenter[0], area.getY() - myClusterCenter[1]) / this.maxDistToCluster;
                        score *= (1 - distanceFromCluster);
                }
                area.baseScore += score;
        }
        
        HashSet<EntityID> visitedAreas = new HashSet<>();
        private void decreasePoliceAreasScore(double score) {
                if(! visitedAreas.contains(ai.getPosition())){
                        this.areas.get(ai.getPosition()).secondaryScore += 0.2 * score;
                        visitedAreas.add(ai.getPosition());
                }
        }

        private void setAliveBlockadesScore(AURAreaGraph area, double d) {
                
        }
        
        private void blockedHumansScore(AURAreaGraph area, double d) {
                
        }

        private void addGasStationScore(AURAreaGraph area, double score) {
                if(! area.isGasStation()){
                        score = 0;
                }
                area.baseScore += score;
        }

        private void addHydrandScore(AURAreaGraph area, double score) {
                if(! area.isHydrant()){
                        score = 0;
                }
                area.baseScore += score;
        }

        private void setDistanceScore(AURAreaGraph area, double score) {
                area.distanceScore = (AURConstants.Agent.VELOCITY / (double) area.getNoBlockadeTravelCost()) * score;
        }

}
