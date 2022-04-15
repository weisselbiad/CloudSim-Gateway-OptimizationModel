package cloudsimMixedPeEnv.performance;

import java.util.List;

import cloudsimMixedPeEnv.Pgpu;
import cloudsimMixedPeEnv.Vgpu;
import cloudsimMixedPeEnv.VgpuScheduler;
import cloudsimMixedPeEnv.VgpuSchedulerFairShare;
import cloudsimMixedPeEnv.performance.models.PerformanceModel;
import cloudsimMixedPeEnv.selection.PgpuSelectionPolicy;

/**
 * * {@link PerformanceVgpuSchedulerFairShare} extends
 * {@link cloudsimMixedPeEnv.VgpuSchedulerFairShare VgpuSchedulerFairShare}
 * to add support for
 * {@link cloudsimMixedPeEnv.performance.models.PerformanceModel
 * PerformanceModels}.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PerformanceVgpuSchedulerFairShare extends VgpuSchedulerFairShare implements PerformanceScheduler<Vgpu> {
	/** The performance model */
	private PerformanceModel<VgpuScheduler, Vgpu> performanceModel;

	/**
	 *      List, PgpuSelectionPolicy) VgpuSchedulerFairShare(int, List,
	 *      PgpuSelectionPolicy)
	 * 
	 * @param performanceModel
	 *            the performance model
	 */
	public PerformanceVgpuSchedulerFairShare(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy);
		this.performanceModel = performanceModel;
	}

	@Override
	public List<Double> getAvailableMips(Vgpu vgpu, List<Vgpu> vgpuList) {
		return this.performanceModel.getAvailableMips(this, vgpu, vgpuList);
	}
}
