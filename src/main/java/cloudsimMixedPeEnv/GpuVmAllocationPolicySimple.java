package cloudsimMixedPeEnv;

import java.util.List;

import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSuitability;
import org.cloudbus.cloudsim.vms.Vm;

/**
 * {@link GpuVmAllocationPolicySimple} extends {@link GpuVmAllocationPolicy} and
 * implements first-fit algorithm for VM placement.
 * 
 * @author Ahmad Siavashi
 *
 */
public abstract class GpuVmAllocationPolicySimple extends GpuVmAllocationPolicy {

	/**
	 * @param list
	 */
	public GpuVmAllocationPolicySimple(List<? extends Host> list) {
		super(list);
	}

	@Override
	public HostSuitability allocateHostForVm(Vm vm) {
		if (!getVmTable().containsKey(vm.getUid())) {
			GpuVm gpuVm = (GpuVm) vm;
			for (Host host : getHostList()) {
				HostSuitability result = allocateHostForVm(vm, host);
				if (result!=null) {
					continue;
				} else if (!gpuVm.hasVgpu() || allocateGpuForVgpu(gpuVm.getVgpu(), (GpuHost) host)) {
					return result;
				}
				deallocateHostForVm(gpuVm);
			}
		}
		return HostSuitability.NULL;
	}


}
