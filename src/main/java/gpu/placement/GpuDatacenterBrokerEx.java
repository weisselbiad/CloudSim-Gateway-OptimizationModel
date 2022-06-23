package gpu.placement;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.events.PredicateType;
import gpu.core.GpuCloudSimTags;
import gpu.power.PowerGpuDatacenterBroker;

/**
 * 
 * An extension to {@link PowerGpuDatacenterBroker} in order to support
 * placement window. It must be used along with {@link GpuDatacenterEx} or its
 * subclasses.
 * 
 * @author Ahmad Siavashi
 *
 */
public class GpuDatacenterBrokerEx extends PowerGpuDatacenterBroker {

	public GpuDatacenterBrokerEx(String name) throws Exception {
		super(name);
	}

	@Override
	protected void finishExecution() {
		for (Integer datacenterId : getDatacenterIdsList()) {
			CloudSim.cancelAll(datacenterId.intValue(), new PredicateType(GpuCloudSimTags.GPU_VM_DATACENTER_PLACEMENT));
		}
		super.finishExecution();
	};

}
