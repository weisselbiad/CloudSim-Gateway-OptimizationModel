package gpu.power.models;

import gpu.power.models.GpuPowerModelLinear;

/**
 * Implements a power model where the power consumption is linear to resource
 * usage. The host will not be power gated when it is idle.
 * 
 * @author Ahmad Siavashi
 *
 */
public class GpuHostPowerModelLinear extends GpuPowerModelLinear {

	/**
	 * gated when it is idle.
	 * 
	 * @param maxPower
	 *            host's peak power
	 * @param staticPowerPercent
	 *            idle power percentage of peak power
	 */
	public GpuHostPowerModelLinear(double maxPower, double staticPowerPercent) {
		super(maxPower, staticPowerPercent);
	}

	@Override
	public double getPower(double utilization) throws IllegalArgumentException {
		if (utilization < 0 || utilization > 1) {
			throw new IllegalArgumentException("Utilization value must be between 0 and 1");
		}
		double power = getStaticPower() + getConstant() * utilization * 100;
		return power;
	}

}
