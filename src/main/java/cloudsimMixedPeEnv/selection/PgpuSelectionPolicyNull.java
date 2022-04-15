package cloudsimMixedPeEnv.selection;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import cloudsimMixedPeEnv.Pgpu;
import cloudsimMixedPeEnv.Vgpu;
import cloudsimMixedPeEnv.VgpuScheduler;

public class PgpuSelectionPolicyNull implements PgpuSelectionPolicy {

	public PgpuSelectionPolicyNull() {
	}

	@Override
	public <T extends VgpuScheduler> Pgpu selectPgpu(Vgpu vgpu, T scheduler, List<? extends Pgpu> pgpuList) {
		throw new NotImplementedException("");
	}

}
