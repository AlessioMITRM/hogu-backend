package us.hogu.service.intefaces;

import us.hogu.controller.dto.response.DistanceResponseDto;
import us.hogu.controller.dto.response.GeoCoordinatesResponseDto;

public interface StadiaMapService {

	DistanceResponseDto calculateDistance(double originLat, double originLon, double destLat, double destLon);

	GeoCoordinatesResponseDto getCoordinatesFromAddress(String address);

}
