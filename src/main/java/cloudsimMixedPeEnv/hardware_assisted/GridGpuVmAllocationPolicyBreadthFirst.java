package cloudsimMixedPeEnv.hardware_assisted;

import java.util.*;
import java.util.function.BiFunction;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSuitability;
import org.cloudbus.cloudsim.vms.Vm;
import cloudsimMixedPeEnv.GpuHost;
import cloudsimMixedPeEnv.GpuVm;
import cloudsimMixedPeEnv.GpuVmAllocationPolicy;
import cloudsimMixedPeEnv.Pgpu;
import cloudsimMixedPeEnv.Vgpu;
import cloudsimMixedPeEnv.VideoCard;
import org.cloudsimplus.autoscaling.VerticalVmScaling;

public class GridGpuVmAllocationPolicyBreadthFirst extends GpuVmAllocationPolicy {

	/**
	 * 
	 * 
	 * @param list
	 */

	private Map<GpuHost, List<Pair<Pgpu, Integer>>> gpuHostPgpus = new HashMap<>();
	private Map<Pgpu, Integer> pgpuProfileMap = new HashMap<>();

	protected static Integer EMPTY = 0;

	public GridGpuVmAllocationPolicyBreadthFirst(List<? extends Host> list) {
		super(list);
		for (GpuHost gpuHost : getGpuHostList()) {
			gpuHostPgpus.put(gpuHost, new ArrayList<>());
			for (VideoCard videoCard : gpuHost.getVideoCardAllocationPolicy().getVideoCards()) {
				for (Pgpu pgpu : videoCard.getVgpuScheduler().getPgpuList()) {
					gpuHostPgpus.get(gpuHost).add(Pair.of(pgpu, EMPTY));
				}
			}
		}
	}

	@Override
	protected void deallocateGpuForVgpu(Vgpu vgpu) {
		Host host = vgpu.getVm().getHost();
		Pgpu pgpu = vgpu.getVideoCard().getVgpuScheduler().getPgpuForVgpu(vgpu);
		super.deallocateGpuForVgpu(vgpu);
		List<Pair<Pgpu, Integer>> pgpuEntities = gpuHostPgpus.get(host);
		Pair<Pgpu, Integer> pgpuEntity = pgpuEntities.stream().filter(x -> x.getKey() == pgpu).findFirst().get();
		pgpuEntities.remove(pgpuEntity);
		pgpuEntities.add(Pair.of(pgpuEntity.getKey(), pgpuEntity.getValue() - 1));
		if (pgpuEntity.getKey().getGddramProvisioner().getAvailableGddram() == pgpuEntity.getKey()
				.getGddramProvisioner().getGddram()) {
			pgpuProfileMap.remove(pgpuEntity.getKey());
		}
	}

	@Override
	public Map<GpuVm, HostSuitability> allocateHostForVms(List<GpuVm> vms) {
		Map<GpuVm, HostSuitability> results = new HashMap<GpuVm, HostSuitability>();
		for (GpuVm vm : vms) {
			HostSuitability result = null;
			if (!vm.hasVgpu()) {
				result = allocateHostForVm(vm);
			} else {
				result = allocateGpuHostForGpuVm(vm);
			}
			results.put(vm, result);
		}
		return results;

	}

	protected HostSuitability allocateGpuHostForGpuVm(GpuVm vm) {
		for (GpuHost gpuHost : getGpuHostList()) {
			List<Pair<Pgpu, Integer>> pgpuEntities = gpuHostPgpus.get(gpuHost);
			sortPgpusList(pgpuEntities);
			HostSuitability result = allocateHostForVm(vm, gpuHost);
			if (result!=null) {
				for (Pair<Pgpu, Integer> pgpuEntity : pgpuEntities) {
					if (pgpuProfileMap.getOrDefault(pgpuEntity.getKey(), vm.getVgpu().getGddram()) != vm.getVgpu()
							.getGddram()) {
						continue;
					}
					if (allocateGpuHostForVgpu(vm.getVgpu(), gpuHost, pgpuEntity.getKey())!=null) {
						pgpuEntities.remove(pgpuEntity);
						pgpuEntities.add(Pair.of(pgpuEntity.getKey(), pgpuEntity.getValue() + 1));
						pgpuProfileMap.put(pgpuEntity.getLeft(), vm.getVgpu().getGddram());
						return result;
					}
				}
				deallocateHostForVm(vm);
			}
		}
		return null;
	}

	protected void sortPgpusList(List<Pair<Pgpu, Integer>> pgpuList) {
		Collections.sort(pgpuList, new Comparator<Pair<Pgpu, Integer>>() {
			public int compare(Pair<Pgpu, Integer> p1, Pair<Pgpu, Integer> p2) {
				return Integer.compare(p1.getValue(), p2.getValue());
			};
		});
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
		for (Host host : getHostList()) {
			HostSuitability result = allocateHostForVm(vm, host);
			if (result!= null) {
				return result;
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

	@Override
	protected boolean allocateGpuForVgpu(Vgpu vgpu, GpuHost gpuHost) {
		throw new NotImplementedException("not implemented");
	}

}
