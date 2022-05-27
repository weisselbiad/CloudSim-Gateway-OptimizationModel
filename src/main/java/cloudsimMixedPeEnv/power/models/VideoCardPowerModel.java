package cloudsimMixedPeEnv.power.models;

import java.util.Map;

import cloudsimMixedPeEnv.Pgpu;

/**
 * The VideoCardPowerModel interface needs to be implemented in order to provide
 * a model of power consumption of VideoCards, depending on utilization of the
 * Pgpus, GDDRAM and PCIe bandwidth.
 * 
 * @author Ahmad Siavashi
 * 
 */
public interface VideoCardPowerModel {
	public double getPower(Map<Pgpu, Double> pgpuUtilization, Map<Pgpu, Double> gddramUtilization,
			double PCIeBwUtilization);
}
