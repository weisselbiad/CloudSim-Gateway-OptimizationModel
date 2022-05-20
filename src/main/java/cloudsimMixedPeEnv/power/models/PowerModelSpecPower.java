package cloudsimMixedPeEnv.power.models;

import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelHost;

public abstract class PowerModelSpecPower extends PowerModelHost implements PowerModel {

    public double getPower(double utilization) throws IllegalArgumentException {
        if (utilization < 0 || utilization > 1) {
            throw new IllegalArgumentException("Utilization value must be between 0 and 1");
        }
        if (utilization % 0.1 == 0) {
            return getPowerData((int) (utilization * 10));
        }
        int utilization1 = (int) Math.floor(utilization * 10);
        int utilization2 = (int) Math.ceil(utilization * 10);
        double power1 = getPowerData(utilization1);
        double power2 = getPowerData(utilization2);
        double delta = (power2 - power1) / 10;
        double power = power1 + delta * (utilization - (double) utilization1 / 10) * 100;
        return power;
    }

    /**
     * Gets the power consumption for a given utilization percentage.
     *
     * @param index the utilization percentage in the scale from [0 to 10],
     * where 10 means 100% of utilization.
     * @return the power consumption for the given utilization percentage
     */
    protected abstract double getPowerData(int index);

}
