package cloudsimMixedPeEnv.power.models;
import cloudsimMixedPeEnv.power.models.PowerModelSpecPower;
import org.cloudbus.cloudsim.power.PowerMeasurement;

public class PowerModelSpecPowerHpProLiantMl110G5Xeon3075  extends PowerModelSpecPower {


        /**
         * The power consumption according to the utilization percentage.
         * @see #getPowerData(int)
         */
        private final double[] power = { 93.7, 97, 101, 105, 110, 116, 121, 125, 129, 133, 135 };

    @Override
    protected double getPowerData(int index) {
            return power[index];
        }


    @Override
    public PowerMeasurement getPowerMeasurement() {
        return null;
    }
}
