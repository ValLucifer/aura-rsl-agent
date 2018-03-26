package AUR.util.ambulance.ProbabilityDeterminant;

import AUR.util.ambulance.Information.BuildingInfo;
import AUR.util.ambulance.Information.RescueInfo;
import AUR.util.knd.AURAreaGraph;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

/**
 * Created by armanaxh on 3/17/18.
 */
public class BuildingRateDeterminer {

    public static double calc(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building){
        double rate = 0;


        if( ignoreBulding(wsg, rescueInfo, building)){
            return rate;
        }


        rate += clusterEffect(wsg, rescueInfo, building, 1);
        rate += TravelCostToBuildingEffect(wsg, rescueInfo, building, 0.4);
        rate += distanceFromFireEffect(wsg, rescueInfo, building, 0.2);
        rate += broknessEffect(wsg, rescueInfo, building, 0.35);
        rate += buildingTemperatureEffect(wsg, rescueInfo, building, 0.2);
        rate += distanceFromRefugeEffect(wsg, rescueInfo, building, 0.15);
        rate += otherAgentPossionEffect(wsg, rescueInfo, building, 0.7);

        if(rate >= 1){
            rate += TravelCostToBuildingEffect(wsg, rescueInfo, building, 0.7);
            rate += distanceFromRefugeInSearchEffect(wsg, rescueInfo, building, 0.2);

        }
        // more effectess
        // agent Position in Bulding
        // distance from Cluster without
        // distance form Refuge
        // distance form Gas Station
        // Civilian Rally
        // Builidng hayii ke aval Sim tosh agent hast


        return rate;
    }

    public static boolean ignoreBulding(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building){

        if(rescueInfo == null || wsg == null || building == null){
            return true;
        }
        if(building.me.getURN().equals(StandardEntityURN.REFUGE)
                || building.me.isOnFire()
                || (building.me.isFierynessDefined() && building.me.getFieryness() == 8)
                ||  (building.me.isBrokennessDefined() && building.me.getBrokenness() == 0)){
            return true;
        }
        if(rescueInfo.visitedList.contains(building)){
            return true;
        }
        AURAreaGraph areaB = wsg.getAreaGraph(building.me.getID());
        if(areaB.isBuilding()){
            if(areaB.getBuilding().fireSimBuilding.isOnFire() || areaB.getBuilding().fireSimBuilding.getEstimatedFieryness() == 8 ){
                return true;
            }
        }
        if(building.me.isOnFire() || (building.me.isFierynessDefined() && building.me.getFieryness() == 8) ){
            return true;
        }
        if(TravelCostToBuildingEffect(wsg, rescueInfo, building, 1) < 0 ){
            return true;
        }

        return false;
    }
    public static double clusterEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){

        for(StandardEntity entity : rescueInfo.clusterEntity){
            if(entity.getID().equals(building.me.getID())){
                return 1* coefficient;
            }
        }
        return 0;
    }

    public static double TravelCostToBuildingEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        double tempRate = Math.pow(rescueInfo.maxTravelCost - building.travelCostTobulding , 2);
        return (tempRate / Math.pow(rescueInfo.maxTravelCost, 2) )*coefficient;
    }

    public static double distanceFromFireEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        double tempRate = rescueInfo.maxTravelCost;
        return (tempRate/ rescueInfo.maxTravelCost)*coefficient;
    }

    public static double broknessEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        if(building.me.isBrokennessDefined()) {
            double tempRate = Math.pow(RescueInfo.maxBrokness - building.me.getBrokenness() , 2);
            return (tempRate / Math.pow(RescueInfo.maxBrokness, 2) ) * coefficient;
        }
        return 0.5 * coefficient;
    }

    public static double buildingTemperatureEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        if(building.me.isTemperatureDefined()) {
            double tempRate = Math.pow(RescueInfo.maxTemperature - building.me.getTemperature(),2);
            return (tempRate / Math.pow(RescueInfo.maxTemperature,2) ) * coefficient;
        }
        return 0.6 * coefficient;
    }
    public static double distanceFromRefugeEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        double tempRate = rescueInfo.maxDistance - building.distanceFromRefuge ;
        return (tempRate/ rescueInfo.maxDistance)*coefficient;
    }
    public static double distanceFromRefugeInSearchEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        int maxD = 0;
        for(BuildingInfo b : rescueInfo.searchList){
            if(b.distanceFromRefuge > maxD){
                maxD = b.distanceFromRefuge;
            }
        }
        if(maxD == 0){
            return 0;
        }
        double tempRate = maxD - building.distanceFromRefuge ;
        return (tempRate/ maxD)*coefficient;
    }
    //TODO
    public static double otherAgentPossionEffect(AURWorldGraph wsg, RescueInfo rescueInfo, BuildingInfo building , double coefficient){
        for(StandardEntity entity : wsg.wi.getEntitiesOfType(
                StandardEntityURN.AMBULANCE_TEAM,
                StandardEntityURN.FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE)){
            if(entity instanceof Human){
                Human agent = (Human)entity;
                if(agent.isPositionDefined() && agent.getPosition().equals(building.me.getID())){
                    return 1D*coefficient;
                }
            }
        }

        return 0;
    }
}