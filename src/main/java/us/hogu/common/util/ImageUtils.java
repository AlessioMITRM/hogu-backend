package us.hogu.common.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ImageUtils {
	public static final String STORAGE_ROOT = "/storage/";
	public static final String PNG_FORMAT = ".png";
	public static final String JPG_FORMAT = ".jpg";
	public static final String JPEG_FORMAT = ".jpeg";

	

    private static final ObjectMapper objectMapper = new ObjectMapper();
	
    public static String convertImagesToJson(List<String> images) {
        if (images == null || images.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(images);
        } catch (Exception e) {
            return "[]";
        }
    }
	
	public static List<String> parseImagePaths(String imagesJson) {
	    if (imagesJson == null || imagesJson.trim().isEmpty()) {
	        return Collections.emptyList();
	    }
	    
	    try {
	        return Arrays.stream(imagesJson.split(","))
	            .map(String::trim)
	            .filter(path -> !path.isEmpty())
	            .collect(Collectors.toList());
	    } catch (Exception e) {
	        return Collections.emptyList();
	    }
	}
}
