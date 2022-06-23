package gpu.remote;

import gpu.Pgpu;
import gpu.Vgpu;
import gpu.VgpuScheduler;
import gpu.VgpuSchedulerFairShareEx;
import gpu.performance.PerformanceScheduler;
import gpu.performance.PerformanceVgpuSchedulerFairShareEx;
import gpu.performance.models.PerformanceModel;
import gpu.selection.PgpuSelectionPolicy;

import java.util.List;

/**
 * Extends {@link PerformanceVgpuSchedulerFairShareEx} to add support for GPU remoting.
 * 
 * @author Ahmad Siavashi
 * 
 * @see S. Iserte, R. Peña-Ortiz, J. Gutiérrez-Aguado, J. M. Claver and R. Mayo, "GSaaS: A Service to Cloudify and Schedule GPUs," in IEEE Access, vol. 6, pp. 39762-39774, 2018, doi: 10.1109/ACCESS.2018.2855261.
 * 
 */
public class RemoteVgpuSchedulerFairShareEx extends PerformanceVgpuSchedulerFairShareEx
		implements PerformanceScheduler<Vgpu> {

	/**
	 * Extends {@link PerformanceVgpuSchedulerFairShareEx} to add support for GPU remoting.
	 * 
	 * @see {@link VgpuSchedulerFairShareEx}
	 */
	public RemoteVgpuSchedulerFairShareEx(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy, performanceModel);
	}

	@Override
	public boolean isSuitable(Pgpu pgpu, Vgpu vgpu) {
		final int gddramShare = vgpu.getCurrentRequestedGddram();
		final long bwShare = vgpu.getCurrentRequestedBw();
		if (!pgpu.getGddramProvisioner().isSuitableForVgpu(vgpu, gddramShare)
				|| !pgpu.getBwProvisioner().isSuitableForVgpu(vgpu, bwShare)) {
			return false;
		}
		List<Vgpu> pgpuVgpus = getPgpuVgpuMap().get(pgpu);
		if (pgpuVgpus.isEmpty()) {
			return true;

		}
		if (RemoteVgpuTags.isExclusive(vgpu)) {
			return false;

		}
		if (RemoteVgpuTags.isExclusive(pgpuVgpus.get(0))) {
			return false;
		}

		return true;
	}

}
