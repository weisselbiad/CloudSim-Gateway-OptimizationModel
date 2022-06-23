package gpu.power.models;

import gpu.Pgpu;

import java.util.Map;

/**
 * Implements a power model for which zeroes out the video card power
 * consumption.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class VideoCardPowerModelNull implements VideoCardPowerModel {

	/**
	 * Implements a power model for which zeroes out the video card power
	 * consumption. *
	 */
	public VideoCardPowerModelNull() {
	}

	@Override
	public double getPower(Map<Pgpu, Double> pgpuUtilization, Map<Pgpu, Double> gddramUtilization,
			double bwUtilization) {
		return 0;
	}

}
