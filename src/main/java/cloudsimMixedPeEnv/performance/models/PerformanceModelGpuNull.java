/**
 * 
 */
package cloudsimMixedPeEnv.performance.models;

import java.util.List;

import cloudsimMixedPeEnv.Vgpu;
import cloudsimMixedPeEnv.VgpuScheduler;

/**
 * {@link PerformanceModelGpuNull} does not impose any performance
 * degradation on Vgpus sharing a Pgpu.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PerformanceModelGpuNull implements PerformanceModel<VgpuScheduler, Vgpu> {

	/**
	 * This class does not impose any performance degradation on Vgpus sharing a Pgpu.
	 */
	public PerformanceModelGpuNull() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cloudsimMixedPeEnv.interference.models.PerformanceModel#
	 * getAvailableMips(cloudsimMixedPeEnv.GpuHost, java.util.List)
	 */
	@Override
	public List<Double> getAvailableMips(VgpuScheduler scheduler, Vgpu vgpu, List<Vgpu> vgpus) {
		return scheduler.getAllocatedMipsForVgpu(vgpu);
	}

}
