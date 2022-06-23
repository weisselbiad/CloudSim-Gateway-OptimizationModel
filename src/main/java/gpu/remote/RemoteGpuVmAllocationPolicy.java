package gpu.remote;

import org.cloudbus.cloudsim.Host;
import gpu.GpuHost;
import gpu.GpuVm;
import gpu.GpuVmAllocationPolicy;
import gpu.Vgpu;

import java.util.List;

/**
 * This class extends {@link GpuVmAllocationPolicy} to add support for GPU
 * remoting.
 * 
 * @author Ahmad Siavashi
 *
 */
public abstract class RemoteGpuVmAllocationPolicy extends GpuVmAllocationPolicy {

	/**
	 * This class extends {@link GpuVmAllocationPolicy} to add support for GPU
	 * remoting.
	 * 
	 * @see {@link GpuVmAllocationPolicy}
	 */
	public RemoteGpuVmAllocationPolicy(List<? extends Host> list) {
		super(list);

	}

	/**
	 * Are VM and vGPU allocated on different hosts?
	 * 
	 * @param vm
	 * @return
	 */
	public boolean hasRemoteVgpu(GpuVm vm) {
		GpuHost host = (GpuHost) vm.getHost();
		Vgpu vgpu = vm.getVgpu();
		if (vgpu == null || host.getVideoCardAllocationPolicy().getVideoCards().contains(vgpu.getVideoCard())) {
			return false;
		}
		return true;
	}

}
