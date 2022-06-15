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

public abstract class GridGpuVmAllocationPolicyVird extends GpuVmAllocationPolicy {

	/**
	 * First-fit vGPU increasing requests decreasing (VIRD) heuristic
	 * 
	 * @param list
	 */

	private Map<Pgpu, GpuHost> pgpuGpuHostMap = new HashMap<>();
	private List<Pair<Pgpu, Integer>> pgpuListS = new ArrayList<>();
	private List<Pair<Pgpu, Integer>> pgpuListU = new ArrayList<>();
	
	private List<Integer> pgpuProfiles = Arrays.asList(512, 1024, 2048, 4096, 8192);

	protected static Integer EMPTY = 0;

	public GridGpuVmAllocationPolicyVird(List<? extends Host> list) {
		super(list);
		for (GpuHost gpuHost : getGpuHostList()) {
			for (VideoCard videoCard : gpuHost.getVideoCardAllocationPolicy().getVideoCards()) {
				for (Pgpu pgpu : videoCard.getVgpuScheduler().getPgpuList()) {
					pgpuListU.add(Pair.of(pgpu, EMPTY));
					pgpuGpuHostMap.put(pgpu, gpuHost);
				}
			}
		}
	}

	@Override
	protected void deallocateGpuForVgpu(Vgpu vgpu) {
		Pgpu pgpu = vgpu.getVideoCard().getVgpuScheduler().getPgpuForVgpu(vgpu);
		super.deallocateGpuForVgpu(vgpu);
		if (pgpu.getGddramProvisioner().getAvailableGddram() == pgpu.getGddramProvisioner().getGddram()) {
			Pair<Pgpu, Integer> pgpuEntry = pgpuListS.stream().filter(x -> x.getKey() == pgpu).findFirst().get();
			pgpuListS.remove(pgpuEntry);
			pgpuListU.add(Pair.of(pgpu, EMPTY));
		}
	}

	@Override
	public Map<GpuVm, HostSuitability> allocateHostForVms(List<GpuVm> vms) {
		Map<GpuVm, HostSuitability> results = new HashMap<GpuVm, HostSuitability>();
		// Sort VMs in descending order according to associated vGPU
		sortVms(vms);
		sortPgpusListAsc(pgpuListS);
		for (GpuVm vm : vms) {
			HostSuitability result = null;

			if (!vm.hasVgpu()) {
				for (Host host : getHostList()) {
					result = allocateHostForVm(vm, host);
					if (result!=null) {
						break;
					}
				}
				results.put(vm, result);
				continue;
			}

			int vgpuGddram = vm.getVgpu().getGddram();

			// List of used GPUs
			for (Pair<Pgpu, Integer> pgpuEntity : pgpuListS) {
				Pgpu pgpu = pgpuEntity.getKey();
				Integer pgpuProfile = pgpuEntity.getValue();
				if (pgpu.getGddramProvisioner().getAvailableGddram() >= pgpuProfile && vm.getVgpu().getGddram() <= pgpuProfile) {
					// Allocate VM on Pgpu's host
					vm.getVgpu().setGddram(pgpuProfile);
					result = allocateVmOnPgpuHost(vm, pgpu);
					if (result!=null) {
						results.put(vm, result);
						break;
					}
				}
				vm.getVgpu().setGddram(vgpuGddram);
			}

			if (result!=null) {
				continue;
			}

			Pair<Pgpu, Integer> selectedPgpuEntity = null;
			// List of unused GPUs
			for (Pair<Pgpu, Integer> pgpuEntity : pgpuListU) {
				Pgpu pgpu = pgpuEntity.getKey();
				Integer pgpuProfile = getPgpuProfiles().stream().filter(p -> p >= vm.getVgpu().getGddram()).findFirst().orElse(EMPTY);
				if (pgpuProfile != EMPTY && pgpu.getGddramProvisioner().getAvailableGddram() >= pgpuProfile) {
					// Allocate VM on Pgpu's host
					vm.getVgpu().setGddram(pgpuProfile);
					result = allocateVmOnPgpuHost(vm, pgpu);
					if (result!=null) {
						selectedPgpuEntity = pgpuEntity;
						break;
					}
				}
				vm.getVgpu().setGddram(vgpuGddram);
			}

			if (selectedPgpuEntity != null) {
				pgpuListU.remove(selectedPgpuEntity);
				pgpuListS.add(Pair.of(selectedPgpuEntity.getKey(), vm.getVgpu().getGddram()));
			}
			
			results.put(vm, result);
		}
		return results;

	}

	protected HostSuitability allocateVmOnPgpuHost(GpuVm vm, Pgpu pgpu) {
		GpuHost pgpuHost = pgpuGpuHostMap.get(pgpu);
		HostSuitability result = allocateHostForVm(vm, pgpuHost);
		if (result!=null) {
			result = allocateGpuHostForVgpu(vm.getVgpu(), pgpuHost, pgpu);
			if (result!=null) {
				return result;
			}
			deallocateHostForVm(vm);
		}
		return null;
	}

	protected void sortPgpusListAsc(List<Pair<Pgpu, Integer>> pgpuList) {
		Collections.sort(pgpuList, new Comparator<Pair<Pgpu, Integer>>() {
			public int compare(Pair<Pgpu, Integer> p1, Pair<Pgpu, Integer> p2) {
				return Integer.compare(p1.getValue(), p2.getValue());
			};
		});
	}

	/**
	 * Sort VMs in decreasing order according to their attached vGPU
	 * 
	 * @param vms
	 */
	protected void sortVms(List<GpuVm> vms) {
		Collections.sort(vms, Collections.reverseOrder(new Comparator<GpuVm>() {
			@Override
			public int compare(GpuVm vm1, GpuVm vm2) {
				int vgpu1gddram = !vm1.hasVgpu() ? 0 : vm1.getVgpu().getGddram();
				int vgpu2gddram = !vm2.hasVgpu() ? 0 : vm2.getVgpu().getGddram();
				return Integer.compare(vgpu1gddram, vgpu2gddram);
			}
		}));
	}


	@Override
	protected boolean allocateGpuForVgpu(Vgpu vgpu, GpuHost gpuHost) {
		throw new NotImplementedException("not implemented");
	}

	public List<Integer> getPgpuProfiles() {
		return pgpuProfiles;
	}

	public void setPgpuProfiles(List<Integer> pgpuProfiles) {
		this.pgpuProfiles = pgpuProfiles;
		Collections.sort(this.pgpuProfiles);
	}

}
