package gpu.power.models;

import org.cloudbus.cloudsim.power.PowerMeasurement;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelHost;

/**
 * Implements a power model where the power consumption is zeroed out.
 * 
 * @author Ahmad Siavashi
 *
 */
public class GpuHostPowerModelNull extends PowerModelHost implements PowerModel {

	/**
	 * The host will be zeroed out.
	 */
	public GpuHostPowerModelNull() {
	}

	@Override
	public double getPower(double utilization) throws IllegalArgumentException {
		return 0;
	}

	@Override
	public PowerMeasurement getPowerMeasurement() {
		return null;
	}
}
