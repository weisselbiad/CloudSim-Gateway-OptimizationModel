package cloudsimMixedPeEnv.hardware_assisted;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;

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

public class GridGpuVmAllocationPolicyBestFit extends GpuVmAllocationPolicy {

	private List<Host> nonGpuHostList = new ArrayList<>();

	public GridGpuVmAllocationPolicyBestFit(List<? extends Host> list) {
		super(list);
		// Create nonGpuHost list
		for (Host host : list) {
			GpuHost pm = (GpuHost) host;
			if (pm.getVideoCardAllocationPolicy() == null
					|| pm.getVideoCardAllocationPolicy().getVideoCards().isEmpty()) {
				nonGpuHostList.add(pm);
			}
		}
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
			Vgpu vgpu = gpuVm.getVgpu();
			// Case 1 - VM with GPU tasks
			if (vgpu != null) {
				memoryAwareSortGpuHost(getGpuHostList());
				for (GpuHost pm : getGpuHostList()) {
					if (pm.isSuitableForVm(gpuVm)) {
						if (allocateGpuForVgpu(vgpu, pm)) {
							return allocateHostForVm(gpuVm, pm);

						}
					}
				}
				// Case 2 - VM with no GPU task
			} else {
				// Search nonGpuHost for nonGpuVm
				for (Host host : nonGpuHostList) {
					HostSuitability Alo = allocateHostForVm(gpuVm, host);
					if (Alo!=null) {
						return Alo;
					}
				}
				// Search GpuHost for nonGpuVm
				for (GpuHost pm : getGpuHostList()) {
					HostSuitability Alo = allocateHostForVm(gpuVm, pm);
					if (Alo!=null) {
						return Alo;
					}
				}
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
	public <T extends Host> List<T> getSimpleHostList() {
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
	public int getHostCountForParallelSearch() {
		return 0;
	}

	@Override
	public void setHostCountForParallelSearch(int hostCountForParallelSearch) {

	}

	@Override
	protected boolean allocateGpuForVgpu(Vgpu vgpu, GpuHost gpuHost) {
		if (!getVgpuHosts().containsKey(vgpu)) {
			for (VideoCard videoCard : gpuHost.getVideoCardAllocationPolicy().getVideoCards()) {
				for (Entry<Pgpu, List<Vgpu>> entry : videoCard.getVgpuScheduler().getPgpuVgpuMap().entrySet()) {
					Pgpu pgpu = entry.getKey();
					List<Vgpu> vgpus = entry.getValue();
					if (vgpus.isEmpty() || vgpus.get(0).getGddram() == vgpu.getGddram()) {
						HostSuitability result = gpuHost.vgpuCreate(vgpu, pgpu);
						if (result!=null) {
							getVgpuHosts().put(vgpu, gpuHost);
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	protected void memoryAwareSortGpuHost(List<GpuHost> gpuHostList) {
		Collections.sort(gpuHostList, new Comparator<GpuHost>() {
			@Override
			public int compare(GpuHost h1, GpuHost h2) {
				Integer minGpuMemory1 = h1.getVideoCardAllocationPolicy().getVideoCards().stream()
						.map(x -> x.getVgpuScheduler().getMinAvailableMemory()).mapToInt(v -> v).min()
						.orElseThrow(NoSuchElementException::new);
				Integer minGpuMemory2 = h2.getVideoCardAllocationPolicy().getVideoCards().stream()
						.map(x -> x.getVgpuScheduler().getMinAvailableMemory()).mapToInt(v -> v).min()
						.orElseThrow(NoSuchElementException::new);
				return Integer.compare(minGpuMemory1, minGpuMemory2);
			}
		});
	}

}
