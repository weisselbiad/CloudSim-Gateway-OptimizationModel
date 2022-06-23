/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.brokers;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.List;

/**
 * A simple implementation of {@link DatacenterBroker} that try to host customer's VMs
 * at the first Datacenter found. If there isn't capacity in that one,
 * it will try the other ones.
 *
 * <p>The default selection of VMs for each cloudlet is based on a Round-Robin policy,
 * cyclically selecting the next VM from the broker VM list for each requesting
 * cloudlet.
 * However, when {@link #setSelectClosestDatacenter(boolean) selection of the closest datacenter}
 * is enabled, the broker will try to place each VM at the closest Datacenter as possible,
 * according to their timezone.</p>
 *
 * <p>Such a policy doesn't check if the selected VM is really suitable for the Cloudlet
 * and may not provide an optimal mapping.</p>
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Toolkit 1.0
 *
 * @see DatacenterBrokerFirstFit
 * @see DatacenterBrokerBestFit
 * @see DatacenterBrokerHeuristic
 */
public class DatacenterBrokerSimple extends DatacenterBrokerAbstract {
    /**
     * Index of the last VM selected from the {@link #getVmExecList()}
     * to run some Cloudlet.
     */
    private int lastSelectedVmIndex;

    /**
     * Index of the last Datacenter selected to place some VM.
     */
    private int lastSelectedDcIndex;

    /**
     * Creates a new DatacenterBroker.
     *
     * @param simulation the CloudSim instance that represents the simulation the Entity is related to
     */
    public DatacenterBrokerSimple(final CloudSim simulation) {
        this(simulation, "");
    }

    /**
     * Creates a DatacenterBroker giving a specific name.
     *
     * @param simulation the CloudSim instance that represents the simulation the Entity is related to
     * @param name the DatacenterBroker name
     */
    public DatacenterBrokerSimple(final CloudSim simulation, final String name) {
        super(simulation, name);
        this.lastSelectedVmIndex = -1;
        this.lastSelectedDcIndex = -1;
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>It applies a Round-Robin policy to cyclically select
     * the next Datacenter from the list. However, it just moves
     * to the next Datacenter when the previous one was not able to create
     * all {@link #getVmWaitingList() waiting VMs}.</p>
     *
     * <p>This policy is just used if the selection of the closest Datacenter is not enabled.
     * Otherwise, the {@link #closestDatacenterMapper(Datacenter, Vm)} is used instead.</p>
     *
     * @param lastDatacenter {@inheritDoc}
     * @param vm {@inheritDoc}
     * @return {@inheritDoc}
     * @see DatacenterBroker#setDatacenterMapper(java.util.function.BiFunction)
     * @see #setSelectClosestDatacenter(boolean)
     */
    @Override
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

    @Override
    public List<? extends Cloudlet> getCloudletSubmittedList() {
        return cloudletSubmittedList;
    }

    @Override
    public void bindgpuCloudletToVm() {

    }
}
