package gpu.remote;

import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import gpu.GpuHost;
import gpu.GpuVm;
import gpu.Pgpu;
import gpu.VideoCard;

import java.util.*;
import java.util.Map.Entry;

/**
 * This class extends {@link RemoteGpuVmAllocationPolicySimple} and allocates
 * GPU-enabled VMs on GPU hosts with least loaded (i.t.o. allocated memory)
 * pGPUs.
 * 
 *
 * @author Ahmad Siavashi
 *
 */
public class RemoteGpuVmAllocationPolicyLeastLoad extends RemoteGpuVmAllocationPolicySimple {

	/**
	 * This class extends {@link RemoteGpuVmAllocationPolicySimple} and allocates
	 * GPU-enabled VMs on GPU hosts with least loaded (i.t.o. allocated memory)
	 * pGPUs.
	 * 
	 * @see {@link RemoteGpuVmAllocationPolicySimple}
	 */
	public RemoteGpuVmAllocationPolicyLeastLoad(List<? extends Host> list) {
		super(list);
	}

	@Override
	public boolean allocateHostForVm(Vm vm) {
		if (((GpuVm) vm).hasVgpu()) {
			sortGpuHosts();
		}
		return super.allocateHostForVm(vm);
	}

	/**
	 * Return available memory of each video card's least loaded pGPU.
	 */
	public Map<VideoCard, Map<Pgpu, Integer>> getVideoCardsAvailableMemory(GpuHost gpuHost) {
		Map<VideoCard, Map<Pgpu, Integer>> videoCardsAvailableMemory = new HashMap<>();
		for (VideoCard videoCard : gpuHost.getVideoCardAllocationPolicy().getVideoCards()) {
			videoCardsAvailableMemory.put(videoCard, videoCard.getVgpuScheduler().getPgpusAvailableMemory());
		}
		return videoCardsAvailableMemory;
	}

	protected void sortGpuHosts() {
		Collections.sort(getGpuHostList(), Collections.reverseOrder(new Comparator<GpuHost>() {
			public int compare(GpuHost gpuHost1, GpuHost gpuHost2) {
				Integer host1MaxAvailableGpuMemory = 0;
				Integer host2MaxAvailableGpuMemory = 0;
				for (Entry<VideoCard, Map<Pgpu, Integer>> item : getVideoCardsAvailableMemory(gpuHost1).entrySet()) {
					Integer videoCardMaxAvailableMemory = Collections.max(item.getValue().values());
					host1MaxAvailableGpuMemory = videoCardMaxAvailableMemory > host1MaxAvailableGpuMemory
							? videoCardMaxAvailableMemory
							: host1MaxAvailableGpuMemory;
				}
				for (Entry<VideoCard, Map<Pgpu, Integer>> item : getVideoCardsAvailableMemory(gpuHost2).entrySet()) {
					Integer videoCardMaxAvailableMemory = Collections.max(item.getValue().values());
					host2MaxAvailableGpuMemory = videoCardMaxAvailableMemory > host2MaxAvailableGpuMemory
							? videoCardMaxAvailableMemory
							: host2MaxAvailableGpuMemory;
				}
				return Integer.compare(host1MaxAvailableGpuMemory, host2MaxAvailableGpuMemory);
			};
		}));
	}

}
