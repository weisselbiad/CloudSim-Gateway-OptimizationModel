package cloudsimMixedPeEnv;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyAbstract;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Comparator.comparing;

public class VmAllocationPolicyGpuAware extends VmAllocationPolicyAbstract implements VmAllocationPolicy {
    private int lastHostIndex;
    private int lastGpuHostIndex;

    /**
     * Instantiates a VmAllocationPolicySimple.
     */
    public VmAllocationPolicyGpuAware() {
        super();
    }

    /**
     * Instantiates a VmAllocationPolicySimple, changing the {@link Function} to select a Host for a Vm
     * in order to define a different policy.
     *
     * @param findHostForVmFunction a {@link Function} to select a Host for a given Vm.
     * @see VmAllocationPolicy#setFindHostForVmFunction(java.util.function.BiFunction)
     */
    public VmAllocationPolicyGpuAware(final BiFunction<VmAllocationPolicy, Vm, Optional<Host>> findHostForVmFunction) {
        super(findHostForVmFunction);
    }

    /**
     * Gets the first suitable host from the {@link #getHostList()} that has the fewest number of used PEs (i.e, higher free PEs).
     *
     * @return an {@link Optional} containing a suitable Host to place the VM or an empty {@link Optional} if not found
     */
    @Override
    protected Optional<Host> defaultFindHostForVm(final Vm vm) {


        final var hostList = getHostList();
        final ArrayList SimpleList = new ArrayList<>();
        final ArrayList GpuList = new ArrayList<>();
        final int maxTries = hostList.size();
        for (Host host : hostList) {
            if (host instanceof HostSimple ) {
                SimpleList.add(host);
            } else if (host instanceof GpuHost) {
                GpuList.add(host);
            }
        }

        for (int i = 0; i < maxTries; i++) {
            final Host gpuhost = (Host) GpuList.get(lastGpuHostIndex);
            final Host simplehost = (Host) SimpleList.get(lastHostIndex);

            lastHostIndex = ++lastHostIndex % SimpleList.size();
            lastGpuHostIndex = ++lastGpuHostIndex % GpuList.size();
            //Different from the FirstFit policy, it always increments the host index.
            if (vm instanceof GpuVm) {

                if (gpuhost.isSuitableForVm(vm)) {

                    return Optional.of(gpuhost);
                }
            } else if (vm instanceof VmSimple) {


                if (simplehost.isSuitableForVm(vm)) {

                    return Optional.of(simplehost);
                }

            }

        }return Optional.empty();
    }
}

