package gpu.performance;

import gpu.Pgpu;
import gpu.Vgpu;
import gpu.VgpuScheduler;
import gpu.VgpuSchedulerSpaceShared;
import gpu.performance.models.PerformanceModel;
import gpu.selection.PgpuSelectionPolicy;

import java.util.List;

/**
 * {@link PerformanceVgpuSchedulerSpaceShared} extends
 * {@link gpu.VgpuSchedulerSpaceShared
 * VgpuSchedulerSpaceShared} to add support for
 * {@link gpu.performance.models.PerformanceModel
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
	 * @see gpu.VgpuSchedulerSpaceShared#VgpuSchedulerSpaceShared(int,
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
