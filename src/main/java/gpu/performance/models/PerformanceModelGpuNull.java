/**
 * 
 */
package gpu.performance.models;

import gpu.Vgpu;
import gpu.VgpuScheduler;

import java.util.List;

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
	 * @see gpu.interference.models.PerformanceModel#
	 * getAvailableMips(gpu.GpuHost, java.util.List)
	 */
	@Override
	public List<Double> getAvailableMips(VgpuScheduler scheduler, Vgpu vgpu, List<Vgpu> vgpus) {
		return scheduler.getAllocatedMipsForVgpu(vgpu);
	}

}
