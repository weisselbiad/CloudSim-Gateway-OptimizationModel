package cloudsimMixedPeEnv;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;


public class MixedDatacenterBroker extends BrokerAbstract {
    /**
     * Index of the last VM selected from the {@link #getVmExecList()}
     * to run some Cloudlet.lastSelectedDcIndex
     */
    private int lastSelectedVmIndex;

    /**
     * Index of the last Datacenter selected to place some VM.
     */
    private int lastSelectedDcIndex;
    private ArrayList<Vm> cutVmList  = new ArrayList<>();
    private  List<Vm> vmcreatedlist = getVmCreatedList();
    /**
     * Creates a new DatacenterBroker.
     *
     * @param simulation the CloudSim instance that represents the simulation the Entity is related to
     */
    public MixedDatacenterBroker(final CloudSim simulation) {
        this(simulation, "");
    }

    /**
     * Creates a DatacenterBroker giving a specific name.
     *lastSelectedDcIndex
     * @param simulation the CloudSim instance that represents the simulation the Entity is related to
     * @param name the DatacenterBroker name
     */
    public MixedDatacenterBroker(final CloudSim simulation, final String name) {
        super(simulation, name);
        this.lastSelectedVmIndex = -1;
        this.lastSelectedDcIndex = -1;
    }

    protected Datacenter defaultDatacenterMapper(final Datacenter lastDatacenter, final Vm vm) {
        if(getDatacenterList().isEmpty()) {
            throw new IllegalStateException("You don't have any Datacenter created.");
        }

        if (lastDatacenter != Datacenter.NULL) {
            return getDatacenterList().get(lastSelectedDcIndex);
        }

        /*If all Datacenter were tried already, return Datacenter.NULL to indicate
         * there isn't a suitable Datacenter to place waiting VMs.*/
        if(lastSelectedDcIndex == getDatacenterList().size()-1){
            return Datacenter.NULL;
        }

        return getDatacenterList().get(++lastSelectedDcIndex);
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>It applies a Round-Robin policy to cyclically select
     * the next Vm from the {@link #getVmWaitingList() list of waiting VMs}.</p>
     *
     * @param cloudlet {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected Vm defaultVmMapper(final Cloudlet cloudlet) {
        if (cloudlet.isBoundToVm()) {
            return cloudlet.getVm();
        }

        if (getVmExecList().isEmpty()) {
            return Vm.NULL;
        }

        /*If the cloudlet isn't bound to a specific VM or the bound VM was not created,
        cyclically selects the next VM on the list of created VMs.*/
        lastSelectedVmIndex = ++lastSelectedVmIndex % getVmExecList().size();
        return getVmFromCreatedList(lastSelectedVmIndex);
    }

    protected Vm MyVmMapper (final Cloudlet cloudlet){
        if (cloudlet.isBoundToVm()) {
            return cloudlet.getVm();
        }

        if (getVmExecList().isEmpty()) {
            return Vm.NULL;
        }


        if(cloudlet instanceof GpuCloudlet) {

            lastSelectedVmIndex = ++lastSelectedVmIndex % getVmExecList().size();
            //cutVmList.addAll((vmcreatedlist));
            ArrayList<Vm> col = new ArrayList<>();
            //col = new ArrayList<>(this.vmcreatedlist);
            cutVmList.addAll((vmcreatedlist));
           // col = (ArrayList<Vm>) cutVmList.subList(vmcreatedlist.size() - lastSelectedVmIndex, vmcreatedlist.size()).stream().filter(vm -> vm instanceof GpuVm).toList();
            for (Vm vm: cutVmList){
                if (vm instanceof GpuVm ) {
                    col.add(vm);
                    System.out.println("Gpu vm  ::::: "+vm);
                } else if (vm instanceof VmSimple) {
                    continue;
                }}

            System.out.println("col Size ::::: "+col.size());


            Vm Gpuvm = col.get(1);
            col.clear();
            cutVmList.clear();

            return Gpuvm;

        }else{
            lastSelectedVmIndex = ++lastSelectedVmIndex % getVmExecList().size();
            getVmCreatedList().forEach(System.out::println);
            return getVmFromCreatedList(lastSelectedVmIndex);

        }}
}




