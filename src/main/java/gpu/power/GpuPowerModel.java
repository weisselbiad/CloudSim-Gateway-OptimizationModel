package gpu.power;

public interface GpuPowerModel {
    double getPower(double utilization) throws IllegalArgumentException;
}
