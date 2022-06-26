package gpu.power.models;

import gpu.power.GpuPowerModel;

public class GpuPowerModelLinear implements GpuPowerModel {
    /** The max power that can be consumed. */
    private double maxPower;

    /** The constant that represents the power consumption
     * for each fraction of resource used. */
    private double constant;

    /** The static power consumption that is not dependent of resource usage.
     * It is the amount of energy consumed even when the host is idle.
     */
    private double staticPower;

    /**
     * Instantiates a new linear power model.
     *
     * @param maxPower the max power
     * @param staticPowerPercent the static power percent
     */
    public GpuPowerModelLinear(double maxPower, double staticPowerPercent) {
        setMaxPower(maxPower);
        setStaticPower(staticPowerPercent * maxPower);
        setConstant((maxPower - getStaticPower()) / 100);
    }

    @Override
    public double getPower(double utilization) throws IllegalArgumentException {
        if (utilization < 0 || utilization > 1) {
            throw new IllegalArgumentException("Utilization value must be between 0 and 1");
        }
        if (utilization == 0) {
            return 0;
        }
        return getStaticPower() + getConstant() * utilization * 100;
    }

    /**
     * Gets the max power.
     *
     * @return the max power
     */
    protected double getMaxPower() {
        return maxPower;
    }

    /**
     * Sets the max power.
     *
     * @param maxPower the new max power
     */
    protected void setMaxPower(double maxPower) {
        this.maxPower = maxPower;
    }

    /**
     * Gets the constant.
     *
     * @return the constant
     */
    protected double getConstant() {
        return constant;
    }

    /**
     * Sets the constant.
     *
     * @param constant the new constant
     */
    protected void setConstant(double constant) {
        this.constant = constant;
    }

    /**
     * Gets the static power.
     *
     * @return the static power
     */
    protected double getStaticPower() {
        return staticPower;
    }

    /**
     * Sets the static power.
     *
     * @param staticPower the new static power
     */
    protected void setStaticPower(double staticPower) {
        this.staticPower = staticPower;
    }

}
