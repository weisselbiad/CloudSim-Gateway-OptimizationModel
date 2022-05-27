package cloudsimMixedPeEnv.selection;

import java.util.List;

import cloudsimMixedPeEnv.Pgpu;
import cloudsimMixedPeEnv.Vgpu;
import cloudsimMixedPeEnv.VgpuScheduler;

/**
 * {@link PgpuSelectionPolicySimple} implements {@link PgpuSelectionPolicy} and
 * selects the first Pgpu in the list.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PgpuSelectionPolicySimple implements PgpuSelectionPolicy {

	/**
	 * Returns the first Pgpu or null if no choice has been provided.
	 */
	public PgpuSelectionPolicySimple() {
	}

	@Override
	public <T extends VgpuScheduler> Pgpu selectPgpu(Vgpu vgpu, T scheduler, List<? extends Pgpu> pgpuList) {
		if (pgpuList.isEmpty()) {
			return null;
		}
		return pgpuList.get(0);
	}

}
