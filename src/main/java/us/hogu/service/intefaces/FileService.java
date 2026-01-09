package us.hogu.service.intefaces;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import us.hogu.model.enums.ServiceType;

public interface FileService {

	void uploadImages(Long id, ServiceType serviceType, List<String> imageNames, List<MultipartFile> images)
			throws IOException;

	void updateImages(Long serviceId, ServiceType serviceType, List<String> oldImageNames,
			List<MultipartFile> newImages) throws IOException;

	void uploadImagesPathCustom(Path basePath, List<String> imageNames, List<MultipartFile> images) throws IOException;

	void updateImagesPathCustom(Path basePath, List<String> oldImageNames, List<MultipartFile> newImages)
			throws IOException;

}
