package cloudsimMixedPeEnv;

import java.util.Random;

import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;

public class UtilizationModelGaussian implements UtilizationModel{

	private Random random;
	private double mean;
	private double std;
	
	public UtilizationModelGaussian(double mean, double std) {
		setMean(mean);
		setStd(std);
		setRandom(new Random());
	}
	
	public UtilizationModelGaussian(double mean, double std, long seed) {
		setMean(mean);
		setStd(std);
		setRandom(new Random(seed));
	}
	
	public UtilizationModelGaussian(long seed) {
		this(0, 1, seed);
	}
	
	public UtilizationModelGaussian() {
		this(0, 1);
	}

	@Override
	public Simulation getSimulation() {
		return null;
	}

	@Override
	public Unit getUnit() {
		return null;
	}

	@Override
	public UtilizationModel setSimulation(Simulation simulation) {
		return null;
	}

	@Override
	public double getUtilization(double time) {
		return getRandom().nextGaussian() * getStd() + getMean();
	}

	@Override
	public double getUtilization() {
		return 0;
	}

	@Override
	public boolean isOverCapacityRequestAllowed() {
		return false;
	}

	@Override
	public UtilizationModel setOverCapacityRequestAllowed(boolean allow) {
		return null;
	}

	public Random getRandom() {
		return random;
	}

	protected void setRandom(Random random) {
		this.random = random;
	}

	public double getMean() {
		return mean;
	}

	protected void setMean(double mean) {
		this.mean = mean;
	}

	public double getStd() {
		return std;
	}

	protected void setStd(double std) {
		this.std = std;
	}
	
}
