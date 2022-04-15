package cloudsimMixedPeEnv;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSuitability;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.autoscaling.VerticalVmScaling;

/**
 * {@link GpuVmAllocationPolicySimple} extends {@link GpuVmAllocationPolicy} and
 * implements first-fit algorithm for VM placement.
 * 
 * @author Ahmad Siavashi
 *
 */
public class GpuVmAllocationPolicySimple extends GpuVmAllocationPolicy {

	/**
	 * @param list
	 */
	public GpuVmAllocationPolicySimple(List<? extends Host> list) {
		super(list);
	}

	@Override
	public Datacenter getDatacenter() {
		return null;
	}

	@Override
	public void setDatacenter(Datacenter datacenter) {

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
		return null;
	}

	@Override
	public <T extends Vm> List<T> allocateHostForVm(Collection<T> vmCollection) {
		return null;
	}

	@Override
	public boolean scaleVmVertically(VerticalVmScaling scaling) {
		return false;
	}

	@Override
	public void setFindHostForVmFunction(BiFunction<VmAllocationPolicy, Vm, Optional<Host>> findHostForVmFunction) {

	}

	@Override
	public <T extends Host> List<T> getHostList() {
		return null;
	}

	@Override
	public Map<Vm, Host> getOptimizedAllocationMap(List<? extends Vm> vmList) {
		return null;
	}

	@Override
	public Optional<Host> findHostForVm(Vm vm) {
		return Optional.empty();
	}

	@Override
	public boolean isVmMigrationSupported() {
		return false;
	}

	@Override
	public boolean isParallelHostSearchEnabled() {
		return super.isParallelHostSearchEnabled();
	}

	@Override
	public int getHostCountForParallelSearch() {
		return 0;
	}

	@Override
	public void setHostCountForParallelSearch(int hostCountForParallelSearch) {

	}
}
