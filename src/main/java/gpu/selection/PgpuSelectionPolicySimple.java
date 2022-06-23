package gpu.selection;

import gpu.Pgpu;
import gpu.Vgpu;
import gpu.VgpuScheduler;

import java.util.List;

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
	public Pgpu selectPgpu(Vgpu vgpu, VgpuScheduler scheduler, List<Pgpu> pgpuList) {
		if (pgpuList.isEmpty()) {
			return null;
		}
		return pgpuList.get(0);
	}

}
