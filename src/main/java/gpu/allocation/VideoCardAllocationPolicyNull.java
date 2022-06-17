package gpu.allocation;

import org.apache.commons.lang3.NotImplementedException;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VideoCard;

import java.util.List;

public class VideoCardAllocationPolicyNull extends VideoCardAllocationPolicy {

	public VideoCardAllocationPolicyNull(List<? extends VideoCard> videoCards) {
		super(videoCards);
	}

	@Override
	public boolean allocate(Vgpu vgpu, int PCIeBw) {
		throw new NotImplementedException("");
	}

	@Override
	public boolean allocate(VideoCard videoCard, Vgpu vgpu, int PCIeBw) {
		throw new NotImplementedException("");
	}

}
