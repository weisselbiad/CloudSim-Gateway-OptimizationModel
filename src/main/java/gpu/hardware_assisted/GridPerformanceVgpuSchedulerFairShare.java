package gpu.hardware_assisted;

import gpu.Pgpu;
import gpu.Vgpu;
import gpu.VgpuScheduler;
import gpu.performance.PerformanceScheduler;
import gpu.performance.models.PerformanceModel;
import gpu.selection.PgpuSelectionPolicy;

import java.util.List;

/**
 * * {@link GridPerformanceVgpuSchedulerFairShare} extends
 * {@link gpu.VgpuSchedulerFairShare VgpuSchedulerFairShare}
 * to add support for
 * {@link gpu.performance.models.PerformanceModel
 * PerformanceModels}.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GridPerformanceVgpuSchedulerFairShare extends GridVgpuSchedulerFairShare implements PerformanceScheduler<Vgpu> {
	/** The performance model */
	private PerformanceModel<VgpuScheduler, Vgpu> performanceModel;

	/**
	 * @see gpu.VgpuSchedulerFairShare#VgpuSchedulerFairShare(int,
	 *      List, PgpuSelectionPolicy) VgpuSchedulerFairShare(int, List,
	 *      PgpuSelectionPolicy)
	 * 
	 * @param performanceModel
	 *            the performance model
	 */
	public GridPerformanceVgpuSchedulerFairShare(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy);
		this.performanceModel = performanceModel;
	}

	@Override
	public List<Double> getAvailableMips(Vgpu vgpu, List<Vgpu> vgpuList) {
		return this.performanceModel.getAvailableMips(this, vgpu, vgpuList);
	}
}
