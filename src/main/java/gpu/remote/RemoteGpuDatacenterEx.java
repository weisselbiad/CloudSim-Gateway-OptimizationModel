package gpu.remote;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import gpu.BusTags;
import gpu.GpuCloudlet;
import gpu.GpuTask;
import gpu.GpuVm;
import gpu.core.GpuCloudSimTags;
import gpu.placement.GpuDatacenterBrokerEx;
import gpu.placement.GpuDatacenterEx;

import java.util.List;

/**
 * 
 * This class extends {@link GpuDatacenterEx} to support remote vGPUs. It must
 * be used along with {@link GpuDatacenterBrokerEx} or its subclasses.
 * 
 * @author Ahmad Siavashi
 *
 */
public class RemoteGpuDatacenterEx extends GpuDatacenterEx {

	public RemoteGpuDatacenterEx(String name, DatacenterCharacteristics characteristics,
			VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList, double schedulingInterval,
			double placementWindow) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, placementWindow);
	}

	protected boolean hasRemoteGpuOverhead(GpuTask gt) {
		GpuVm vm = getGpuTaskVm(gt);
		if (getVmAllocationPolicy() instanceof RemoteGpuVmAllocationPolicy) {
			RemoteGpuVmAllocationPolicy vmAllocationPolicy = (RemoteGpuVmAllocationPolicy) getVmAllocationPolicy();
			if (vmAllocationPolicy.hasRemoteVgpu(vm)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void processGpuMemoryTransfer(SimEvent ev) {
		GpuTask gt = (GpuTask) ev.getData();

		double bandwidth = Double.valueOf(BusTags.PCI_E_3_X16_BW);

		if (gt.getStatus() == GpuTask.CREATED) {
			double delay = gt.getTaskInputSize() / bandwidth;
			send(getId(), delay, GpuCloudSimTags.GPU_TASK_SUBMIT, gt);
		} else if (gt.getStatus() == GpuTask.SUCCESS) {
			double delay = gt.getTaskOutputSize() / bandwidth;
			GpuCloudlet cl = gt.getCloudlet();
			if (hasRemoteGpuOverhead(gt)) {
				delay = gt.getCommunicationOverhead() / 100.0 * (CloudSim.clock() - cl.getExecStartTime() + delay);
			}
			send(getId(), delay, GpuCloudSimTags.GPU_CLOUDLET_RETURN, cl);
		}
	}

}
