package cloudsimMixedPeEnv.selection;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cloudsimMixedPeEnv.Pgpu;
import cloudsimMixedPeEnv.Vgpu;
import cloudsimMixedPeEnv.VgpuScheduler;

/**
 * {@link PgpuSelectionPolicyBestFit} implements {@link PgpuSelectionPolicy} and
 * selects the Pgpu which best fits a given vGPU.
 * 
 * @author Ahmad Siavashi
 *
 */
public class PgpuSelectionPolicyBestFit implements PgpuSelectionPolicy {

	public PgpuSelectionPolicyBestFit() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cloudsimMixedPeEnv.selection.PgpuSelectionPolicy#selectPgpu(org.
	 * cloudbus.cloudsim.gpu.VgpuScheduler, java.util.List)
	 */
	@Override
	public <T extends VgpuScheduler> Pgpu selectPgpu(Vgpu vgpu, T scheduler, List<? extends Pgpu> pgpuList) {
		if (pgpuList.isEmpty()) {
			return null;
		}
		return Collections.min(pgpuList, new Comparator<Pgpu>() {
			@Override
			public int compare(Pgpu pgpu1, Pgpu pgpu2) {
				int pgpu1AvailableMemory = pgpu1.getGddramProvisioner().getAvailableGddram();
				int pgpu2AvailableMemory = pgpu2.getGddramProvisioner().getAvailableGddram();
				return Integer.compare(pgpu1AvailableMemory, pgpu2AvailableMemory);
			}
		});
	}

}
