package cloudsimMixedPeEnv.selection;

import java.util.List;

import cloudsimMixedPeEnv.Pgpu;
import cloudsimMixedPeEnv.Vgpu;
import cloudsimMixedPeEnv.VgpuScheduler;

/**
 * When a new Vgpu is to be allocated on a videoCard, there may be multiple
 * Pgpus that it can reside on. The PgpuSelectionPolicy is an interface that
 * needs to be implemented to provide a policy for selecting a Pgpu from
 * possible Pgpus in the videoCard.
 * 
 * @author Ahmad Siavashi
 * 
 */
public interface PgpuSelectionPolicy {

	/**
	 * Selects a Pgpu from the given list of Pgpus according to the specified
	 * policy.
	 * @param vgpu
	 *			  the vgpu that is being allocated 
	 * @param scheduler
	 *            the vgpuScheduler
	 * @param pgpuList
	 *            list of possible choices
	 * @return a pgpu that is selected from the list of pgpus according to the
	 *         specified policy
	 */
	public <T extends VgpuScheduler> Pgpu selectPgpu(final Vgpu vgpu, final T scheduler, final List<? extends Pgpu> pgpuList);
}
