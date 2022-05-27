package cloudsimMixedPeEnv.hardware_assisted;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.cloudbus.cloudsim.hosts.Host;
import cloudsimMixedPeEnv.GpuVm;

public class GridGpuVmAllocationPolicyViri extends GridGpuVmAllocationPolicyVird {

	/**
	 * First-fit vGPU increasing requests increasing (VIRD) heuristic
	 * 
	 * @param list
	 */

	public GridGpuVmAllocationPolicyViri(List<? extends Host> list) {
		super(list);
	}

	@Override
	protected void sortVms(List<GpuVm> vms) {
		Collections.sort(vms, new Comparator<GpuVm>() {
			@Override
			public int compare(GpuVm vm1, GpuVm vm2) {
				int vgpu1gddram = !vm1.hasVgpu() ? 0 : vm1.getVgpu().getGddram();
				int vgpu2gddram = !vm2.hasVgpu() ? 0 : vm2.getVgpu().getGddram();
				return Integer.compare(vgpu1gddram, vgpu2gddram);
			}
		});
	}

}
