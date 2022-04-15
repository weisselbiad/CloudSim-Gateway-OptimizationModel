package cloudsimMixedPeEnv.performance;

import java.util.List;

import cloudsimMixedPeEnv.Pgpu;
import cloudsimMixedPeEnv.Vgpu;
import cloudsimMixedPeEnv.VgpuScheduler;
import cloudsimMixedPeEnv.VgpuSchedulerSpaceShared;
import cloudsimMixedPeEnv.performance.models.PerformanceModel;
import cloudsimMixedPeEnv.selection.PgpuSelectionPolicy;

/**
 * {@link PerformanceVgpuSchedulerSpaceShared} extends
 * {@link cloudsimMixedPeEnv.VgpuSchedulerSpaceShared
 * VgpuSchedulerSpaceShared} to add support for
 * {@link cloudsimMixedPeEnv.performance.models.PerformanceModel
 * PerformanceModels}.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PerformanceVgpuSchedulerSpaceShared extends VgpuSchedulerSpaceShared
		implements PerformanceScheduler<Vgpu> {

	/** The performance model */
	private PerformanceModel<VgpuScheduler, Vgpu> performanceModel;

	/**
	 *      List, PgpuSelectionPolicy) VgpuSchedulerSpaceShared(int, List,
	 *      PgpuSelectionPolicy)
	 * @param performanceModel
	 *            the performance model
	 */
	public PerformanceVgpuSchedulerSpaceShared(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy);
		this.performanceModel = performanceModel;
	}

	@Override
	public List<Double> getAvailableMips(Vgpu vgpu, List<Vgpu> vgpuList) {
		return this.performanceModel.getAvailableMips(this, vgpu, vgpuList);
	}

}
