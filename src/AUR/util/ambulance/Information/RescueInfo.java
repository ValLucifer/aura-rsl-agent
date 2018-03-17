package AUR.util.ambulance.Information;


import AUR.util.knd.AURGeoUtil;
import AUR.util.knd.AURWorldGraph;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.component.module.AbstractModule;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import sun.applet.resources.MsgAppletViewer;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;
import static rescuecore2.standard.entities.StandardEntityURN.POLICE_OFFICE;

/**
 * Created by armanaxh on 3/3/18.
 */
public class RescueInfo extends AbstractModule {


    // const *****************************
    public static final int simulationTime = 420;
    public static final int maxBuriedness = 100;
    public static final int maxDamage = 500;
    public static final int maxHp = 10000;
    public static final int thresholdRestDmg = 60;
    public static final int gasStationExplosionRange = 50000;
    public static int maxTravelTime = 50;
    public static int moveDistance = 40000;
    public static final int maxDistanceFromFire = Integer.MAX_VALUE;
    public static final int maxBrokness = 100;
    public static final int maxTemperature = 47;
    public  int losDamge;
    public  int losHp;
    private boolean initB = false;

    public AURWorldGraph wsg;
    public AmbulanceInfo ambo;
    public int agentSpeed;

    //Detector
    public Set<StandardEntity> clusterEntity;
    public HashMap<EntityID, CivilianInfo> civiliansInfo;
    public HashMap<EntityID, RefugeInfo> refugesInfo;
    public Set<CivilianInfo> canNotRescueCivilian;

    //Search
    public Map<EntityID, BuildingInfo> buildingInfo;


    public RescueInfo(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData){
        super(ai, wi, si, moduleManager, developData);
        this.ambo = new AmbulanceInfo((AmbulanceTeam)ai.me());
        this.refugesInfo = new HashMap<>();
        this.civiliansInfo = new HashMap<>();
        this.clusterEntity = new HashSet<>();
        this.canNotRescueCivilian = new HashSet<>();
        this.buildingInfo = new HashMap<>();
        this.wsg = moduleManager.getModule("knd.AuraWorldGraph", "AUR.util.knd.AURWorldGraph");
        this.wsg.rescueInfo = this;
    }


    // init *************************************************************************************

    private void init(){
        this.agentSpeed = 30000;
        this.losDamge = wsg.si.getPerceptionLosPrecisionDamage();
        this.losHp = wsg.si.getPerceptionLosPrecisionHp();
        this.initRefuge();
        this.initBulding();
    }

    private void initBulding(){

        for(StandardEntity entity : worldInfo.getEntitiesOfType(
                BUILDING,
                GAS_STATION,
                AMBULANCE_CENTRE,
                FIRE_STATION,
                POLICE_OFFICE ))
        {
            if(entity instanceof Building) {

                Building b = (Building)entity;

                this.buildingInfo.put(entity.getID(), new BuildingInfo(wsg, this , b));

            }
        }
    }

    private void initRefuge(){

        for(StandardEntity entity: wsg.wi.getEntitiesOfType(StandardEntityURN.REFUGE)){
            if(entity instanceof Refuge){
                Refuge refuge = (Refuge)entity;
                RefugeInfo refugeInfo = new RefugeInfo(refuge, this);
                this.refugesInfo.put(refuge.getID(), refugeInfo);
            }
        }

    }

    public RescueInfo initCalc(){
        if(!initB) {
            init();
            initB = true;
        }
        return this;
    }

    // Update ***********************************************************************************
    public void updateInformation(){

        if(agentInfo.getTime() > 1 && !this.initB){
            init();
            initB = true;
        }

        this.updateCycle();
        this.updateChanges();
        this.updateBuildingInfo();

    }

    private void updateBuildingInfo(){
        for(BuildingInfo b : this.buildingInfo.values()){
            b.updateInformation();
        }
    }

    private void updateCycle(){
        for(CivilianInfo c : civiliansInfo.values()){
            c.updateCycle();
        }
    }
    private void updateChanges(){
        Set<EntityID> changes = worldInfo.getChanged().getChangedEntities();
        for(EntityID id: changes){
            StandardEntity entity = worldInfo.getEntity(id);
            if(entity.getStandardURN().equals(StandardEntityURN.CIVILIAN)) {
                Civilian civilian = (Civilian) entity;
                updateCivilianInfo(civilian);
                //TODO
            }
        }
        updateViwe();

    }


    public ArrayList<Line2D> testLine = new ArrayList<>();//For debug
    public ArrayList<Edge> areasInter = new ArrayList<>();//For debug

    private void updateViwe(){
//TODO BUGFIX

        testLine.clear();
        areasInter.clear();

        Set<EntityID> change = worldInfo.getChanged().getChangedEntities();
        Set<EntityID> temp = new HashSet<>();
        for(CivilianInfo ci : this.civiliansInfo.values()){
            if(!ci.me.isPositionDefined() || !ci.me.isXDefined() || !ci.me.isYDefined()){
                continue;
            }
            if(change.contains(ci.getPosition())
                    && !change.contains(ci.me.getID())
                    && worldInfo.getDistance(ambo.me, ci.me) <= scenarioInfo.getPerceptionLosMaxDistance() ){

                testLine.add(new Line2D(new Point2D(ambo.me.getX(), ambo.me.getY()) , new Point2D(ci.me.getX(), ci.me.getY()) ));
                boolean intersect = false;
                for(StandardEntity entity : worldInfo.getObjectsInRange( ambo.me , scenarioInfo.getPerceptionLosMaxDistance()) ) {

                    if (entity instanceof Area) {
                        Area area = (Area) entity;


                        if(entity instanceof Road){
                            continue;
                        }
                        for (Edge e : area.getEdges()) {
                            double[] d = new double[2];
                            if (e.isPassable()) {
                                continue;
                            }
                            if (AURGeoUtil.getIntersection(
                                    e.getStartX(), e.getStartY(),
                                    e.getEndX(), e.getEndY(),
                                    ambo.me.getX(), ambo.me.getY(),
                                    ci.me.getX(), ci.me.getY(),
                                    d)) {
                                intersect = true;
                                areasInter.add(e);
                                break;
                            }
                        }
                        if(intersect == true) {
                            break;
                        }
                    }

                }
                if (intersect == false) {
                    temp.add(ci.getID());
                }
            }
        }

        for(EntityID id : temp) {
            civiliansInfo.remove(id);
        }

    }

    private void updateCivilianInfo(Civilian civilian){
        //just when see it

        CivilianInfo civilianInfo = null;
        if(!civiliansInfo.containsKey(civilian.getID())) {
            civilianInfo =  new CivilianInfo(civilian, this);
            civiliansInfo.put(civilian.getID(), civilianInfo);
        }else{
            civilianInfo = civiliansInfo.get(civilian.getID());
        }

        if(civilianInfo != null){
            civilianInfo.updateInformation();
        }

    }


    public void updateAgentSpeed(){

    }

    // Calc ***************************************************************************************
    @Override
    public AbstractModule calc() {
        return null;
    }
}
