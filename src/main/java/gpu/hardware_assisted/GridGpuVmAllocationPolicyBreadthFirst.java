package gpu.hardware_assisted;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import gpu.*;
import gpu.allocation.VideoCardAllocationPolicy;
import gpu.allocation.VideoCardAllocationPolicyNull;
import gpu.selection.PgpuSelectionPolicy;
import gpu.selection.PgpuSelectionPolicyNull;

import java.util.*;

public class GridGpuVmAllocationPolicyBreadthFirst extends GpuVmAllocationPolicy {

	/**
	 * This class extends {@link GpuVmAllocationPolicy} to implement breadth-first
	 * policy and enforce homogeneous vGPU allocation restriction of NVIDIA GRID
	 * technology according to NVIDIA documents.
	 * 
	 * <b>Note</b>: This class performs a global placement, so classes required for
	 * hierarchical placement that extend or implement
	 * {@link VideoCardAllocationPolicy} and {@link PgpuSelectionPolicy} can be set
	 * to {@link VideoCardAllocationPolicyNull} and {@link PgpuSelectionPolicyNull}
	 * respectively. Otherwise, they are ignored.
	 * 
	 * @author Ahmad Siavashi
	 *
	 */

	private Map<GpuHost, List<Pair<Pgpu, Integer>>> gpuHostPgpus = new HashMap<>();
	private Map<Pgpu, Integer> pgpuProfileMap = new HashMap<>();

	protected static final Integer EMPTY = 0;

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
	public Map<GpuVm, Boolean> allocateHostForVms(List<GpuVm> vms) {
		Map<GpuVm, Boolean> results = new HashMap<GpuVm, Boolean>();
		for (GpuVm vm : vms) {
			boolean result = allocateHostForVm(vm);
			results.put(vm, result);
		}
		return results;

	}

	protected boolean allocateGpuHostForGpuVm(GpuVm vm) {
		for (GpuHost gpuHost : getGpuHostList()) {
			List<Pair<Pgpu, Integer>> pgpuEntities = gpuHostPgpus.get(gpuHost);
			sortPgpusList(pgpuEntities);
			boolean result = allocateHostForVm(vm, gpuHost);
			if (result) {
				for (Pair<Pgpu, Integer> pgpuEntity : pgpuEntities) {
					if (pgpuProfileMap.getOrDefault(pgpuEntity.getKey(), vm.getVgpu().getGddram()) != vm.getVgpu()
							.getGddram()) {
						continue;
					}
					if (allocateGpuHostForVgpu(vm.getVgpu(), gpuHost, pgpuEntity.getKey())) {
						pgpuEntities.remove(pgpuEntity);
						pgpuEntities.add(Pair.of(pgpuEntity.getKey(), pgpuEntity.getValue() + 1));
						pgpuProfileMap.put(pgpuEntity.getLeft(), vm.getVgpu().getGddram());
						return true;
					}
				}
				deallocateHostForVm(vm);
			}
		}
		return false;
	}

	protected void sortPgpusList(List<Pair<Pgpu, Integer>> pgpuList) {
		Collections.sort(pgpuList, new Comparator<Pair<Pgpu, Integer>>() {
			public int compare(Pair<Pgpu, Integer> p1, Pair<Pgpu, Integer> p2) {
				return Integer.compare(p1.getValue(), p2.getValue());
			};
		});
	}

	@Override
	public boolean allocateHostForVm(Vm vm) {
		GpuVm gpuVm = (GpuVm) vm;
		if (!gpuVm.hasVgpu()) {
			for (Host host : getHostList()) {
				boolean result = allocateHostForVm(vm, host);
				if (result) {
					return true;
				}
			}
		} else {
			return allocateGpuHostForGpuVm(gpuVm);
		}
		return false;
	}

	@Override
	protected boolean allocateGpuForVgpu(Vgpu vgpu, GpuHost gpuHost) {
		throw new NotImplementedException("not implemented");
	}

}
