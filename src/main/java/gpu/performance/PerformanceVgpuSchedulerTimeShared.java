package gpu.performance;

import gpu.Pgpu;
import gpu.Vgpu;
import gpu.VgpuScheduler;
import gpu.VgpuSchedulerTimeShared;
import gpu.performance.models.PerformanceModel;
import gpu.selection.PgpuSelectionPolicy;

import java.util.List;

/**
 * {@link PerformanceVgpuSchedulerTimeShared} extends
 * {@link VgpuSchedulerTimeShared
 * VgpuSchedulerTimeShared} to add support for
 * {@link gpu.performance.models.PerformanceModel
 * PerformanceModels}.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PerformanceVgpuSchedulerTimeShared extends VgpuSchedulerTimeShared implements PerformanceScheduler<Vgpu> {

	/** The performance model */
	private PerformanceModel<VgpuScheduler, Vgpu> performanceModel;

	/**
	 *      List, PgpuSelectionPolicy) VgpuSchedulerTimeShared(int, List,
	 *      PgpuSelectionPolicy)
	 * @param performanceModel
	 *            the performance model
	 */
	public PerformanceVgpuSchedulerTimeShared(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy);
		this.performanceModel = performanceModel;
	}

	@Override
	public List<Double> getAvailableMips(Vgpu vgpu, List<Vgpu> vgpuList) {
		return this.performanceModel.getAvailableMips(this, vgpu, vgpuList);
	}
}
