package gpu;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.List;
import java.util.Map;

public abstract class GpuVmAllocationPolicyAbstract  {
    /*
     * Title:        CloudSim Toolkit
     * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
     * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
     *
     * Copyright (c) 2009-2012, The University of Melbourne, Australia
     */



        /** The host list. */
        private List<? extends Host> hostList;

        /**
         * Creates a new VmAllocationPolicy object.
         *
         * @pre $none
         * @post $none
         */
        public GpuVmAllocationPolicyAbstract(List<? extends Host> list) {
            setHostList(list);
        }

        /**
         * Allocates a host for a given VM.
         *
         * @param vm the VM to allocate a host to
         * @return $true if the host could be allocated; $false otherwise
         * @pre $none
         * @post $none
         */
        public abstract boolean allocateHostForVm(Vm vm);

        /**
         * Allocates a specified host for a given VM.
         *
         * @param vm virtual machine which the host is reserved to
         * @param host host to allocate the the given VM
         * @return $true if the host could be allocated; $false otherwise
         * @pre $none
         * @post $none
         */
        public abstract boolean allocateHostForVm(Vm vm, Host host);

        /**
         * Optimize allocation of the VMs according to current utilization.
         *
         * @param vmList the vm list
         * @return the array list< hash map< string, object>>
         *
         * @todo It returns a list of maps, where each map key is a string
         * and stores an object. What in fact are the keys and values of this
         * Map? Neither this class or its subclasses implement the method
         *
         */
        public abstract List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList);

        /**
         * Releases the host used by a VM.
         *
         * @param vm the vm to get its host released
         * @pre $none
         * @post $none
         */
        public abstract void deallocateHostForVm(Vm vm);

        /**
         * Get the host that is executing the given VM.
         *
         * @param vm the vm
         * @return the Host with the given vmID; $null if not found
         *
         * @pre $none
         * @post $none
         */
        public abstract Host getHost(Vm vm);

        /**
         * Get the host that is executing the given VM belonging to the given user.
         *
         * @param vmId the vm id
         * @param userId the user id
         * @return the Host with the given vmID and userID; $null if not found
         * @pre $none
         * @post $none
         */
        public abstract Host getHost(int vmId, int userId);

        /**
         * Sets the host list.
         *
         * @param hostList the new host list
         */
        protected void setHostList(List<? extends Host> hostList) {
            this.hostList = hostList;
        }

        /**
         * Gets the host list.
         *
         * @return the host list
         */
        @SuppressWarnings("unchecked")
        public <T extends Host> List<T> getHostList() {
            return (List<T>) hostList;
        }




}
