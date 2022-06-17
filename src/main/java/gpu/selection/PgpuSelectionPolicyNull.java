package gpu.selection;

import org.apache.commons.lang3.NotImplementedException;
import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VgpuScheduler;

import java.util.List;

public class PgpuSelectionPolicyNull implements PgpuSelectionPolicy {

	public PgpuSelectionPolicyNull() {
	}

	@Override
	public Pgpu selectPgpu(Vgpu vgpu, VgpuScheduler scheduler, List<Pgpu> pgpuList) {
		throw new NotImplementedException("");
	}

}
