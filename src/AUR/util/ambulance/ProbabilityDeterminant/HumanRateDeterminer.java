package AUR.util.ambulance.ProbabilityDeterminant;


import AUR.util.ambulance.AmbulanceUtil;
import AUR.util.ambulance.Information.CivilianInfo;
import AUR.util.ambulance.Information.RescueInfo;
import AUR.util.knd.AURWorldGraph;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;

/**
 *
 * @author armanaxh - 2018
 */


public class HumanRateDeterminer {

    private CivilianInfo civilian;
    private AURWorldGraph wsg;

    public HumanRateDeterminer(AURWorldGraph wsg, CivilianInfo ci){
        this.civilian = ci;
        this.wsg = wsg;

    }

    //TODO static
    public void calc(){
        double rate = 0;

        if(ignoreCivilian()){
            civilian.rate = rate;
            return;
        }
        //Base Rate
        rate += effectAreaType(0.01);
        rate += effectReverseSaveCycle(2.3);
        rate += effectDamage(0.5);

        if(rate > 1) {
            //chose rate
            rate += effectHp(0.5);
            rate += effectBuriedness(0.5);
            rate += effectTravelTime(0.25);
            rate += effectTravelTimeToRefuge(0.25);
            rate += clusterEffect(0.25);
            // MY cluster effect
            // Civilian Rally
            // distance of Fire // kamtar bashe behtare chon mohem tare vali bayad Save Time daghig bege key mimire
            //age Raido bege civilian hast chimishe ?

        }


        civilian.rate = rate;
    }

    public boolean ignoreCivilian(){
        //TODO Fuck
        if(civilian.saveTime <= 0){
            return true;
        }
        if(wsg.wi.getEntity(civilian.getPosition()) instanceof AmbulanceTeam){
            return true;
        }

        StandardEntity posEntity = wsg.wi.getEntity(civilian.getPosition());
        if(posEntity.getStandardURN().equals(StandardEntityURN.REFUGE)){
            return true;
        }

        if(posEntity instanceof Building) {
            Building position = (Building)posEntity;
            if (position.isOnFire()) {
                return true;
            }
        }
        return false;
    }

    public double effectAreaType(double coefficient){
        double tempRate = 0;
        if(AmbulanceUtil.inSideBulding(wsg,civilian.me)){
            tempRate += 1;
        }else if(AmbulanceUtil.onTheRoad(wsg, civilian.me)){
            tempRate += 0.3;
        }

        return tempRate * coefficient;
    }


    public double effectReverseSaveCycle(double coefficient){
        double tempRate = RescueInfo.simulationTime - (civilian.saveTime);
        return (tempRate/ RescueInfo.simulationTime)*coefficient;
    }

    public double effectBuriedness(double coefficient){
        if(!civilian.me.isBuriednessDefined()){
            return 0;
        }
        if(civilian.getBuriedness() == 0){
            return 0;
        }
        double tempRate = RescueInfo.maxBuriedness - civilian.getBuriedness();
        return (tempRate/ RescueInfo.maxBuriedness)*coefficient;

    }

    public double effectTravelTime(double coefficient){
        double temprate = RescueInfo.maxTravelTime - civilian.travelTimeToMe;
        return ((temprate*1D)/ RescueInfo.maxTravelTime)*coefficient;
    }

    public double effectTravelTimeToRefuge(double coefficient){
        double temprate = RescueInfo.maxTravelTime - civilian.travelTimeToRefuge;
        return ((temprate*1D)/ RescueInfo.maxTravelTime)*coefficient;
    }

    public double effectDamage(double coefficient){
        double tempRate = civilian.getDmg();
        if(tempRate > 500 ){
            return 0;
        }
        return (tempRate/ RescueInfo.maxDamage)*coefficient;
    }

    public double effectHp(double coefficient){
        double tempRate = RescueInfo.maxHp - civilian.getHp();
        return (tempRate/ RescueInfo.maxHp)*coefficient;
    }

    public double clusterEffect(double coefficient){

        for(StandardEntity entity : wsg.rescueInfo.clusterEntity){
            if(civilian.me.isPositionDefined()) {
                if (entity.getID().equals(civilian.me.getPosition())) {
                    return 1 * coefficient;
                }
            }
        }
        return 0;
    }
}