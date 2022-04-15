package cloudsimMixedPeEnv.performance;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import cloudsimMixedPeEnv.GpuHost;
import cloudsimMixedPeEnv.Vgpu;
import cloudsimMixedPeEnv.allocation.VideoCardAllocationPolicy;
import org.cloudbus.cloudsim.provisioners.ResourceProvisioner;
import org.cloudbus.cloudsim.provisioners.PeProvisioner;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;



/**
 * {@link PerformanceGpuHost} extends {@link GpuHost} to add support for
 * schedulers that implement {@link PerformanceScheduler PerformanceScheduler}
 * interface.
 * 
 * @author Ahmad Siavashi
 *
 */
public class PerformanceGpuHost extends GpuHost {

	/**
	 * @see cloudsimMixedPeEnv.GpuHost#GpuHost GpuHost
	 */
	public PerformanceGpuHost( ResourceProvisioner ramProvisioner, ResourceProvisioner bwProvisioner,
			long storage, List<Pe> peList,
			VideoCardAllocationPolicy videoCardAllocationPolicy) {
		super(ramProvisioner, bwProvisioner, storage, peList, videoCardAllocationPolicy);
	}

	/**
	 * @see cloudsimMixedPeEnv.GpuHost#GpuHost GpuHost
	 */
	public PerformanceGpuHost( long ramProvisioner, long bwProvisioner,
			long storage, List< Pe> peList, VmScheduler vmScheduler, VideoCardAllocationPolicy videoCardAllocationPolicy) {
		super(ramProvisioner, bwProvisioner, storage, peList, vmScheduler, videoCardAllocationPolicy);
		setVideoCardAllocationPolicy(videoCardAllocationPolicy);
		setVmScheduler(vmScheduler);
	}

	@Override
	public double updateVgpusProcessing(double currentTime) {
		double smallerTime = Double.MAX_VALUE;

		if (getVideoCardAllocationPolicy() != null) {
			List<Vgpu> runningVgpus = new ArrayList<Vgpu>();
			for (Vgpu vgpu : getVideoCardAllocationPolicy().getVgpuVideoCardMap().keySet()) {
				if (vgpu.getGpuTaskScheduler().runningTasks() > 0) {
					runningVgpus.add(vgpu);
				}
			}
			for (Vgpu vgpu : getVideoCardAllocationPolicy().getVgpuVideoCardMap().keySet()) {
				@SuppressWarnings("unchecked")
				PerformanceScheduler<Vgpu> vgpuScheduler = (PerformanceScheduler<Vgpu>) getVideoCardAllocationPolicy()
						.getVgpuVideoCardMap().get(vgpu).getVgpuScheduler();
				double time = vgpu.updateGpuTaskProcessing(currentTime,
						vgpuScheduler.getAvailableMips(vgpu, runningVgpus));
				if (time > 0.0 && time < smallerTime) {
					smallerTime = time;
				}
			}
		}

		return smallerTime;
	}

}
