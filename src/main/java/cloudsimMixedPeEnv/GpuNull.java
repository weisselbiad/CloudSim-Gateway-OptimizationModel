package cloudsimMixedPeEnv;

import gpu.GpuVm;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CustomerEntity;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.resources.Processor;
import org.cloudbus.cloudsim.resources.Resource;
import org.cloudbus.cloudsim.resources.ResourceManageable;
import org.cloudbus.cloudsim.schedulers.MipsShare;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.vms.*;
import org.cloudsimplus.autoscaling.HorizontalVmScaling;
import org.cloudsimplus.autoscaling.VerticalVmScaling;
import org.cloudsimplus.listeners.EventListener;
import org.cloudsimplus.listeners.VmDatacenterEventInfo;
import org.cloudsimplus.listeners.VmHostEventInfo;

import java.util.Collections;
import java.util.List;

public class GpuNull extends VmNull implements Vm {


    @Override public void setId(long id) {/**/}
    @Override public long getId() {
        return -1;
    }
    @Override public double getSubmissionDelay() {
        return 0;
    }
    @Override public boolean isDelayed() { return false; }
    @Override public void setSubmissionDelay(double submissionDelay) {/**/}
    @Override public void addStateHistoryEntry(VmStateHistoryEntry entry) {/**/}
    @Override public ResourceManageable getBw() {
        return (ResourceManageable) Resource.NULL;
    }
    @Override public CloudletScheduler getCloudletScheduler() { return CloudletScheduler.NULL; }
    @Override public long getFreePesNumber() { return 0; }
    @Override public long getExpectedFreePesNumber() { return 0; }
    @Override public long getCurrentRequestedBw() {
        return 0;
    }
    @Override public MipsShare getCurrentRequestedMips() {
        return MipsShare.NULL;
    }
    @Override public long getCurrentRequestedRam() {
        return 0;
    }
    @Override public double getTotalCpuMipsRequested() {
        return 0.0;
    }
    @Override public Host getHost() {
        return Host.NULL;
    }
    @Override public double getMips() {
        return 0;
    }
    @Override public long getNumberOfPes() {
        return 0;
    }
    @Override public Vm addOnHostAllocationListener(EventListener<VmHostEventInfo> listener) {
        return this;
    }
    @Override public Vm addOnMigrationStartListener(EventListener<VmHostEventInfo> listener) { return this; }
    @Override public Vm addOnMigrationFinishListener(EventListener<VmHostEventInfo> listener) { return this; }
    @Override public Vm addOnHostDeallocationListener(EventListener<VmHostEventInfo> listener) { return this; }
    @Override public Vm addOnCreationFailureListener(EventListener<VmDatacenterEventInfo> listener) {
        return this;
    }
    @Override public Vm addOnUpdateProcessingListener(EventListener<VmHostEventInfo> listener) {
        return this;
    }
    @Override public void notifyOnHostAllocationListeners() {/**/}
    @Override public void notifyOnHostDeallocationListeners(Host deallocatedHost) {/**/}
    @Override public void notifyOnCreationFailureListeners(Datacenter failedDatacenter) {/**/}
    @Override public boolean removeOnMigrationStartListener(EventListener<VmHostEventInfo> listener) { return false; }
    @Override public boolean removeOnMigrationFinishListener(EventListener<VmHostEventInfo> listener) { return false; }
    @Override public boolean removeOnUpdateProcessingListener(EventListener<VmHostEventInfo> listener) {
        return false;
    }
    @Override public boolean removeOnHostAllocationListener(EventListener<VmHostEventInfo> listener) {
        return false;
    }
    @Override public boolean removeOnHostDeallocationListener(EventListener<VmHostEventInfo> listener) {
        return false;
    }
    @Override public boolean removeOnCreationFailureListener(EventListener<VmDatacenterEventInfo> listener) { return false; }
    @Override public ResourceManageable getRam() {
        return (ResourceManageable) Resource.NULL;
    }
    @Override public Resource getStorage() {
        return Resource.NULL;
    }
    @Override public List<VmStateHistoryEntry> getStateHistory() {
        return Collections.emptyList();
    }
    @Override public double getCpuPercentUtilization() { return 0; }
    @Override public double getCpuPercentUtilization(double time) {
        return 0.0;
    }
    @Override public double getCpuPercentRequested() { return 0; }
    @Override public double getCpuPercentRequested(double time) { return 0; }
    @Override public double getHostCpuUtilization(double time) { return 0; }
    @Override public double getExpectedHostCpuUtilization(double vmCpuUtilizationPercent) { return 0; }
    @Override public double getHostRamUtilization() { return 0; }
    @Override public double getHostBwUtilization() { return 0; }

    @Override
    public double getHostCpuUtilization() {
        return 0;
    }

    @Override public double getTotalCpuMipsUtilization() { return 0; }
    @Override public double getTotalCpuMipsUtilization(double time) {
        return 0.0;
    }
    @Override public String getUid() {
        return "";
    }
    @Override public DatacenterBroker getBroker() {
        return DatacenterBroker.NULL;
    }
    @Override public void setBroker(DatacenterBroker broker) {/**/}
    @Override public double getStartTime() { return 0; }
    @Override public Vm setStartTime(double startTime) { return this; }
    @Override public double getStopTime() { return 0; }
    @Override public double getWaitTime() { return 0; }
    @Override public double getTotalExecutionTime() { return 0; }
    @Override public Vm setStopTime(double stopTime) { return this; }
    @Override public double getLastBusyTime() { return 0; }
    @Override public double getIdleInterval() { return 0; }
    @Override public boolean isIdle() { return false; }
    @Override public boolean isIdleEnough(double time) { return false; }
    @Override public VmResourceStats getCpuUtilizationStats() { return new VmResourceStats(Vm.NULL, vm -> 0.0); }
    @Override public void enableUtilizationStats() {/**/}
    @Override public String getVmm() {
        return "";
    }
    @Override public boolean isCreated() {
        return false;
    }
    @Override public boolean isSuitableForCloudlet(Cloudlet cloudlet) { return false; }
    @Override public boolean isInMigration() {
        return false;
    }
    @Override public void setCreated(boolean created) {/**/}
    @Override public Vm setBw(long bwCapacity) {
        return this;
    }
    @Override public Vm setHost(Host host) { return this; }
    @Override public void setInMigration(boolean migrating) {/**/}
    @Override public Vm setRam(long ramCapacity) {
        return this;
    }
    @Override public Vm setSize(long size) {
        return this;
    }
    @Override public double updateProcessing(double currentTime, MipsShare mipsShare) { return 0.0; }

    @Override
    public double updategpuVmProcessing(double currentTime, List<Double> mipsShare) {
        return 0;
    }

    @Override public double updateProcessing(MipsShare mipsShare) { return 0; }
    @Override public Vm setCloudletScheduler(CloudletScheduler cloudletScheduler) {
        return this;
    }
    @Override public int compareTo(Vm vm) { return 0; }
    @Override public double getTotalMipsCapacity() {
        return 0.0;
    }
    @Override public void setFailed(boolean failed) {/**/}
    @Override public boolean isFailed() {
        return true;
    }
    @Override public boolean isWorking() { return false; }
    @Override public Simulation getSimulation() {
        return Simulation.NULL;
    }
    @Override public void setLastTriedDatacenter(Datacenter lastTriedDatacenter) {/**/}
    @Override public Datacenter getLastTriedDatacenter() { return Datacenter.NULL; }
    @Override public double getArrivedTime() { return 0; }
    @Override public CustomerEntity setArrivedTime(double time) { return this; }
    @Override public double getCreationTime() { return 0; }
    @Override public String toString() { return "Vm.NULL"; }
    @Override public HorizontalVmScaling getHorizontalScaling() { return HorizontalVmScaling.NULL; }
    @Override public Vm setHorizontalScaling(HorizontalVmScaling scaling) throws IllegalArgumentException { return this; }
    @Override public Vm setRamVerticalScaling(VerticalVmScaling scaling) throws IllegalArgumentException { return this; }
    @Override public Vm setBwVerticalScaling(VerticalVmScaling scaling) throws IllegalArgumentException { return this; }
    @Override public Vm setPeVerticalScaling(VerticalVmScaling scaling) throws IllegalArgumentException { return this; }
    @Override public VerticalVmScaling getRamVerticalScaling() { return VerticalVmScaling.NULL; }
    @Override public VerticalVmScaling getBwVerticalScaling() { return VerticalVmScaling.NULL; }
    @Override public VerticalVmScaling getPeVerticalScaling() { return VerticalVmScaling.NULL; }
    @Override public Processor getProcessor() { return Processor.NULL; }

    @Override
    public void setCloudletSequence(List<Cloudlet> CloudletSequenceList) {

    }

    @Override
    public List<Cloudlet> getCloudletSequence() {
        return null;
    }

    @Override public String getDescription() { return ""; }
    @Override public Vm setDescription(String description) { return this; }
    @Override public VmGroup getGroup() { return null; }
    @Override public double getTimeZone() { return Integer.MAX_VALUE; }
    @Override public Vm setTimeZone(double timeZone) { return this; }
    @Override public List<ResourceManageable> getResources() { return Collections.emptyList(); }

}
