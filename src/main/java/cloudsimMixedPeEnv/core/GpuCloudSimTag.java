package cloudsimMixedPeEnv.core;

import cloudsimMixedPeEnv.GpuVm;
import org.cloudbus.cloudsim.core.CloudSimTag;

import java.util.Comparator;

public enum GpuCloudSimTag implements Comparable<GpuCloudSimTag>  {



    /**
     * Contains Gpu-related events in the simulator.
     * {@link org.cloudbus.cloudsim.core.CloudSimTags} cannot be extended due to its
     * private constructor.
     *
     * @author Ahmad Siavashi
     *
     */


        /**
         * Denotes an event to submit a GpuTask for execution.
         */
        GPU_TASK_SUBMIT ,

        /**
         * Denotes an internal event in the GpuDatacenter. Updates the progress of
         * executions.
         */
        VGPU_DATACENTER_EVENT ,

        /**
         * Denotes an event to evaluate the power consumption of a
         * PowerGpuDatacenter}.
         */
        GPU_VM_DATACENTER_POWER_EVENT ,

        /**
         * Denotes an event to perform a {@link GpuVm} placement in a
         */
        GPU_VM_DATACENTER_PLACEMENT ,
        /**
         * Denotes an event to update GPU memory transfers.
         */
        GPU_MEMORY_TRANSFER ,

        /**
         * Denotes the return of a GpuCloudlet to the sender.
         */
         GPU_CLOUDLET_RETURN ;


    public int compare(GpuCloudSimTags o1, GpuCloudSimTags o2) {
        return 0;
    }
}


