package us.hogu.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import us.hogu.common.constants.ErrorConstants;
import us.hogu.common.util.ImageUtils;
import us.hogu.exception.ValidationException;
import us.hogu.model.enums.ServiceType;
import us.hogu.service.intefaces.FileService;

@RequiredArgsConstructor
@Service
public class FileServiceImpl implements FileService {

    @Override
    public void uploadImages(Long id, ServiceType serviceType,
                             List<String> imageNames, List<MultipartFile> images) throws IOException {
        if (images == null || images.isEmpty()) {
            return;
        }

        for (MultipartFile image : images) {
            validateImageFormat(image);

            String fileName = UUID.randomUUID() + "-" + image.getOriginalFilename();
            Path path = Paths.get(
                    ImageUtils.STORAGE_ROOT,
                    serviceType.name().toLowerCase(),
                    id.toString(),
                    fileName
            );
            Files.createDirectories(path.getParent());
            Files.write(path, image.getBytes());
            imageNames.add(fileName);
        }
    }

    @Override
    public void uploadImagesPathCustom(Path basePath, List<String> imageNames, List<MultipartFile> images)
            throws IOException {
        if (images == null || images.isEmpty()) {
            return;
        }

        for (MultipartFile image : images) {
            validateImageFormat(image);

            String fileName = UUID.randomUUID() + "-" + image.getOriginalFilename();
            Path path = basePath.resolve(fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, image.getBytes());
            imageNames.add(fileName);
        }
    }

    @Override
    public void updateImages(Long serviceId, ServiceType serviceType, List<String> oldImageNames,
                             List<MultipartFile> newImages) throws IOException {
        if (newImages == null || newImages.isEmpty()) {
            return; // Non cambia nulla se non ci sono nuove immagini
        }

        Path serviceFolder = Paths.get(ImageUtils.STORAGE_ROOT, serviceType.name().toLowerCase(), serviceId.toString());

        // 1. Cancella TUTTE le vecchie immagini fisicamente
        if (oldImageNames != null && !oldImageNames.isEmpty()) {
            for (String oldImage : oldImageNames) {
                Path oldPath = serviceFolder.resolve(oldImage);
                Files.deleteIfExists(oldPath);
            }
        }

        // 2. Svuota la lista e carica le nuove
        List<String> newImageNames = new ArrayList<>();
        for (MultipartFile image : newImages) {
            validateImageFormat(image);

            String fileName = UUID.randomUUID() + "-" + image.getOriginalFilename();
            Path path = serviceFolder.resolve(fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, image.getBytes());
            newImageNames.add(fileName);
        }

        oldImageNames.clear();
        oldImageNames.addAll(newImageNames);
    }

    @Override
    public void updateImagesPathCustom(Path basePath, List<String> oldImageNames, List<MultipartFile> newImages)
            throws IOException {
        // Se non arrivano nuove immagini, non fare nulla (mantieni le vecchie)
        if (newImages == null || newImages.isEmpty()) {
            return;
        }

        // 1. Cancella TUTTE le vecchie immagini dal disco
        if (oldImageNames != null && !oldImageNames.isEmpty()) {
            for (String oldImage : oldImageNames) {
                Path oldPath = basePath.resolve(oldImage);
                Files.deleteIfExists(oldPath);
            }
        }

        // 2. Prepara la nuova lista di nomi
        List<String> newImageNames = new ArrayList<>();
        for (MultipartFile image : newImages) {
            validateImageFormat(image);

            String fileName = UUID.randomUUID() + "-" + image.getOriginalFilename();
            Path path = basePath.resolve(fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, image.getBytes());
            newImageNames.add(fileName);
        }

        // 3. Sostituisci completamente la lista vecchia con quella nuova
        oldImageNames.clear();
        oldImageNames.addAll(newImageNames);
    }

    // Metodo privato per centralizzare la validazione del formato
    private void validateImageFormat(MultipartFile image) {
        String originalName = image.getOriginalFilename();
        if (originalName == null ||
            !(originalName.toLowerCase().endsWith(ImageUtils.PNG_FORMAT) ||
              originalName.toLowerCase().endsWith(ImageUtils.JPG_FORMAT) ||
              originalName.toLowerCase().endsWith(ImageUtils.JPEG_FORMAT))) {
            throw new ValidationException(ErrorConstants.FORMAT_IMAGE_NOT_ALLOWED.name(),
                    ErrorConstants.FORMAT_IMAGE_NOT_ALLOWED.getMessage());
        }
    }
}