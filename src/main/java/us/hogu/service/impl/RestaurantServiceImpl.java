package us.hogu.service.impl;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.management.ServiceNotFoundException;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import us.hogu.common.constants.ErrorConstants;
import us.hogu.controller.dto.response.InfoStatsDto;
import us.hogu.controller.dto.request.RestaurantBookingRequestDto;
import us.hogu.controller.dto.request.RestaurantServiceRequestDto;
import us.hogu.controller.dto.response.RestaurantBookingResponseDto;
import us.hogu.controller.dto.response.RestaurantManagementResponseDto;
import us.hogu.controller.dto.response.RestaurantServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceDetailResponseDto;
import us.hogu.controller.dto.response.ServiceSummaryResponseDto;
import us.hogu.converter.BookingMapper;
import us.hogu.converter.RestaurantServiceMapper;
import us.hogu.converter.ServiceLocaleMapper;
import us.hogu.exception.UserNotFoundException;
import us.hogu.exception.ValidationException;
import us.hogu.model.RestaurantBooking;
import us.hogu.model.RestaurantServiceEntity;
import us.hogu.model.ServiceLocale;
import us.hogu.model.User;
import us.hogu.model.enums.ServiceType;
import us.hogu.repository.jpa.RestaurantBookingJpa;
import us.hogu.repository.jpa.RestaurantServiceJpa;
import us.hogu.repository.jpa.UserJpa;
import us.hogu.repository.projection.RestaurantDetailProjection;
import us.hogu.repository.projection.RestaurantManagementProjection;
import us.hogu.repository.projection.RestaurantSummaryProjection;
import us.hogu.service.intefaces.FileService;
import us.hogu.controller.dto.request.RestaurantAdvancedSearchRequestDto;
import us.hogu.controller.dto.request.RestaurantAvailabilityRequestDto;
import us.hogu.controller.dto.response.RestaurantAvailabilityResponseDto;
import us.hogu.service.intefaces.RestaurantService;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class RestaurantServiceImpl implements RestaurantService {
    private final RestaurantServiceJpa restaurantServiceRepository;
    private final RestaurantBookingJpa restaurantBookingRepository;
    private final UserJpa userRepository;
    private final FileService fileService;
    private final RestaurantServiceMapper restaurantServiceMapper;
    private final BookingMapper bookingMapper;
    private final ServiceLocaleMapper serviceLocaleMapper;
    
    
    // FRONTEND - lista ristoranti pubblici
    @Override
    @Transactional(readOnly = true)
    public Page<ServiceSummaryResponseDto> getActiveRestaurants(Pageable pageable) {
        String lang = LocaleContextHolder.getLocale().getLanguage();

        Page<RestaurantServiceEntity> entities = restaurantServiceRepository.findActiveByLanguage(lang, pageable);
        List<ServiceSummaryResponseDto> dtoList = new ArrayList<>();

        for (RestaurantServiceEntity entity : entities.getContent()) {
            dtoList.add(restaurantServiceMapper.toSummaryDto(entity));
        }

        return new PageImpl<>(dtoList, pageable, entities.getTotalElements());
    }
    
    // FRONTEND - dettaglio ristorante pubblico
    @Override
    @Transactional(readOnly = true)
    public ServiceDetailResponseDto getRestaurantDetail(Long restaurantId, LocalDate date, Integer numberOfPeople) {

        RestaurantServiceEntity entity = restaurantServiceRepository.findDetailById(restaurantId)
            .orElseThrow(() -> new ValidationException(
                ErrorConstants.RESTURANT_NOT_FOUND.name(),
                ErrorConstants.RESTURANT_NOT_FOUND.getMessage()
            ));

        ServiceDetailResponseDto dto = restaurantServiceMapper.toDetailDto(entity);

        // Verifica disponibilità solo se data e persone sono presenti
        if (date != null && numberOfPeople != null) {

            // Usa mezzanotte o un orario convenzionale, perché non consideri l’ora
            OffsetDateTime dateTime = date.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

            Integer bookedPeople = restaurantBookingRepository.countBookedPeopleByRestaurantAndTime(
                restaurantId,
                dateTime
            );

            int availableCapacity = entity.getCapacity() - bookedPeople;

            dto.setAvailable(availableCapacity >= numberOfPeople);

        } else {
            dto.setAvailable(true); // nessun controllo richiesto
        }

        return dto;
    }
    
    // CLIENTE - prenotazioni ristorante dell'utente
    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantBookingResponseDto> getUserRestaurantBookings(Long userId, Pageable pageable) {
        Page<RestaurantBooking> bookings = restaurantBookingRepository.findByUserId(userId, pageable);
        return bookings.map(bookingMapper::toRestaurantResponseDto);
    }
    
 // FORNITORE - aggiorna ristorante esistente
    @Override
    @Transactional
    public ServiceDetailResponseDto updateRestaurant(Long providerId, Long restaurantId, 
                                                     RestaurantServiceRequestDto request, 
                                                     List<MultipartFile> images) 
    throws IOException 
    {
        // Verifica che il provider esista
        User provider = userRepository.findById(providerId)
            .orElseThrow(() -> new ValidationException(
                ErrorConstants.USER_NOT_FOUND.name(), 
                ErrorConstants.USER_NOT_FOUND.getMessage()
            ));

        // Trova il ristorante e verifica la proprietà
        RestaurantServiceEntity entity = restaurantServiceRepository.findById(restaurantId)
            .orElseThrow(() -> new ValidationException(
                ErrorConstants.RESTURANT_NOT_FOUND.name(), 
                ErrorConstants.RESTURANT_NOT_FOUND.getMessage()
            ));

        if (!entity.getUser().getId().equals(providerId)) {
            throw new ValidationException(
                ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.name(), 
                ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage()
            );
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setMenu(request.getMenu());
        entity.setCapacity(request.getCapacity());
        entity.setBasePrice(request.getBasePrice());
        entity.setLocales(serviceLocaleMapper.mapRequestToEntity(request.getLocales()));
        entity.setPublicationStatus(request.getPublicationStatus());

        fileService.updateImages(entity.getId(), ServiceType.RESTAURANT, entity.getImages(), images);

        RestaurantServiceEntity updatedRestaurant = restaurantServiceRepository.save(entity);
        return restaurantServiceMapper.toDetailDto(updatedRestaurant, provider);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantManagementResponseDto> getProviderRestaurants(Long providerId, Pageable pageable) {
        Page<RestaurantServiceEntity> page = restaurantServiceRepository.findByProviderId(providerId, pageable);
        return page.map(restaurantServiceMapper::toManagementDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RestaurantBookingResponseDto> getRestaurantBookings(Long restaurantId, Long providerId, Pageable pageable) {
        boolean restaurant = restaurantServiceRepository.existsByIdAndUserId(restaurantId, providerId);
        if (!restaurant) {
            throw new ValidationException(
                ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.name(),
                ErrorConstants.RESTURANT_NOT_FOUND_OR_NOT_AUTHORIZED.getMessage()
            );
        }

        Page<RestaurantBooking> bookings = restaurantBookingRepository.findByRestaurantServiceId(restaurantId, pageable);
        return bookings.map(bookingMapper::toRestaurantResponseDto);
    }
    
    // ADMIN - tutti i ristoranti
    @Override
    @Transactional(readOnly = true)
    public List<RestaurantManagementResponseDto> getAllRestaurantsForAdmin() 
    {
    	return restaurantServiceMapper.toManagementDtoList(restaurantServiceRepository.findAllForAdmin());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ServiceSummaryResponseDto> searchRestaurants(String searchTerm) {
        String lang = LocaleContextHolder.getLocale().getLanguage();

        List<RestaurantServiceEntity> entities = restaurantServiceRepository.findActiveBySearchAndLanguage(searchTerm, lang);
        return entities.stream()
            .map(restaurantServiceMapper::toSummaryDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceSummaryResponseDto> advancedSearchRestaurants(RestaurantAdvancedSearchRequestDto searchRequest, Pageable pageable) {
        Specification<RestaurantServiceEntity> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String lang = LocaleContextHolder.getLocale().getLanguage();

            // Join with locales (usalo ovunque)
            Join<RestaurantServiceEntity, ServiceLocale> localesJoin = root.join("locales");

            // Publication status filter
            predicates.add(criteriaBuilder.isTrue(root.get("publicationStatus")));

            // Language filter
            predicates.add(criteriaBuilder.equal(localesJoin.get("language"), lang));

            // Location filter
            if (searchRequest.getLocation() != null && !searchRequest.getLocation().trim().isEmpty()) {
                String location = searchRequest.getLocation().trim().toLowerCase();

                if (location.contains(",")) {
                    String[] parts = location.split(",");
                    if (parts.length == 2) {
                        String city = parts[0].trim().toLowerCase();
                        String state = parts[1].trim().toLowerCase();

                        predicates.add(criteriaBuilder.and(
                            criteriaBuilder.like(criteriaBuilder.lower(localesJoin.get("city")), "%" + city + "%"),
                            criteriaBuilder.like(criteriaBuilder.lower(localesJoin.get("state")), "%" + state + "%")
                        ));
                    } else {
                        predicates.add(criteriaBuilder.or(
                            criteriaBuilder.like(criteriaBuilder.lower(localesJoin.get("city")), "%" + location + "%"),
                            criteriaBuilder.like(criteriaBuilder.lower(localesJoin.get("state")), "%" + location + "%"),
                            criteriaBuilder.like(criteriaBuilder.lower(localesJoin.get("country")), "%" + location + "%")
                        ));
                    }
                } else {
                    predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(localesJoin.get("city")), "%" + location + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(localesJoin.get("state")), "%" + location + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(localesJoin.get("country")), "%" + location + "%")
                    ));
                }
            }

            // Cuisine filter
            if (searchRequest.getCuisine() != null && !searchRequest.getCuisine().isBlank()) {
                predicates.add(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")),
                        "%" + searchRequest.getCuisine().toLowerCase() + "%"
                    )
                );
            }

            // Price range
            if (searchRequest.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("basePrice"), searchRequest.getMinPrice()));
            }
            if (searchRequest.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("basePrice"), searchRequest.getMaxPrice()));
            }

            // Capacity
            if (searchRequest.getNumberOfPeople() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("capacity"), searchRequest.getNumberOfPeople()));
            }

            query.distinct(true);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<RestaurantServiceEntity> restaurants = restaurantServiceRepository.findAll(spec, pageable);

        // Availability filter
        if (searchRequest.getDate() != null && searchRequest.getTime() != null && searchRequest.getNumberOfPeople() != null) {

            OffsetDateTime requestedDateTime = OffsetDateTime.of(
                searchRequest.getDate(),
                searchRequest.getTime(),
                OffsetDateTime.now().getOffset()
            );

            List<ServiceSummaryResponseDto> availableRestaurants = restaurants.getContent().stream()
                .filter(restaurant -> {
                    Integer bookedPeople = restaurantBookingRepository.countBookedPeopleByRestaurantAndTime(
                        restaurant.getId(),
                        requestedDateTime
                    );
                    int availableCapacity = restaurant.getCapacity() - bookedPeople;
                    return availableCapacity >= searchRequest.getNumberOfPeople();
                })
                .map(restaurantServiceMapper::toSummaryDto)
                .collect(Collectors.toList());

            return new PageImpl<>(availableRestaurants, pageable, availableRestaurants.size());
        }

        return restaurants.map(restaurantServiceMapper::toSummaryDto);
    }

    
    @Override
    @Transactional(readOnly = true)
    public RestaurantAvailabilityResponseDto checkRestaurantAvailability(Long restaurantId, RestaurantAvailabilityRequestDto availabilityRequest) {
        RestaurantServiceEntity restaurant = restaurantServiceRepository.findById(restaurantId)
            .orElseThrow(() -> new ValidationException(ErrorConstants.RESTURANT_NOT_FOUND.name(), ErrorConstants.RESTURANT_NOT_FOUND.getMessage()));

        // Check capacity
        boolean isAvailable = availabilityRequest.getNumberOfPeople() <= restaurant.getCapacity();

        // Simulate available time slots (this would typically involve checking existing bookings)
        List<LocalTime> availableSlots = generateAvailableTimeSlots(restaurant, availabilityRequest.getDate(), availabilityRequest.getTime());

        return RestaurantAvailabilityResponseDto.builder()
            .restaurantId(restaurantId)
            .isAvailable(isAvailable)
            .availableTables(restaurant.getCapacity() - availabilityRequest.getNumberOfPeople())
            .maxCapacity(restaurant.getCapacity())
            .nextAvailableSlots(availableSlots)
            .build();
    }
    
    // CLIENTE - crea prenotazione ristorante
    @Override
    @Transactional
    public RestaurantBookingResponseDto createRestaurantBooking(RestaurantBookingRequestDto requestDto, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(), ErrorConstants.USER_NOT_FOUND.getMessage()));
            
        RestaurantServiceEntity restaurant = restaurantServiceRepository.findById(requestDto.getRestaurantServiceId())
            .orElseThrow(() -> new ValidationException(ErrorConstants.RESTURANT_NOT_FOUND.name(), ErrorConstants.RESTURANT_NOT_FOUND.getMessage()));
            
        // Verifica disponibilità
        checkRestaurantAvailability(restaurant, requestDto.getReservationTime(), requestDto.getNumberOfPeople());
        
        RestaurantBooking booking = bookingMapper.toRestaurantEntity(requestDto, user, restaurant);
        RestaurantBooking savedBooking = restaurantBookingRepository.save(booking);
        
        return bookingMapper.toRestaurantResponseDto(savedBooking);
    }
    
    // FORNITORE - crea/aggiorna ristorante
    @Override
    @Transactional
    public ServiceDetailResponseDto createRestaurant(Long providerId, RestaurantServiceRequestDto requestDto,
    												 List<MultipartFile> images) 
    throws IOException
    {
        User provider = userRepository.findById(providerId)
        	.orElseThrow(() -> new ValidationException(ErrorConstants.RESTURANT_NOT_FOUND.name(), ErrorConstants.RESTURANT_NOT_FOUND.getMessage()));
            
        RestaurantServiceEntity restaurant = restaurantServiceMapper.toEntity(requestDto);
        restaurant.setUser(provider);
        restaurant.setCreationDate(OffsetDateTime.now());
        restaurant.setPublicationStatus(true);
        
        RestaurantServiceEntity savedRestaurant = restaurantServiceRepository.save(restaurant);
        savedRestaurant.setImages(new ArrayList<String>());
        
        fileService.uploadImages(savedRestaurant.getId(), ServiceType.RESTAURANT, savedRestaurant.getImages(), images);

        return restaurantServiceMapper.toDetailDto(savedRestaurant, provider);
    }
    
    // ADMIN - approva ristorante
    @Override
    @Transactional
    public void approveRestaurant(Long restaurantId) throws ServiceNotFoundException {
        RestaurantServiceEntity restaurant = restaurantServiceRepository.findById(restaurantId)
            .orElseThrow(() -> new ValidationException(ErrorConstants.RESTURANT_NOT_FOUND.name(), ErrorConstants.RESTURANT_NOT_FOUND.getMessage()));
            
        restaurant.setPublicationStatus(true);
        restaurantServiceRepository.save(restaurant);
    }
    

    private List<LocalTime> generateAvailableTimeSlots(RestaurantServiceEntity restaurant, LocalDate date, LocalTime requestedTime) {
        // This is a simplified implementation. In a real-world scenario, 
        // you would check against existing bookings
        List<LocalTime> slots = new ArrayList<>();
        LocalTime startTime = LocalTime.of(11, 0);  // Lunch start
        LocalTime endTime = LocalTime.of(22, 0);    // Dinner end

        while (startTime.isBefore(endTime)) {
            slots.add(startTime);
            startTime = startTime.plusHours(1);
        }

        return slots;
    }
    
    private void checkRestaurantAvailability(RestaurantServiceEntity restaurant, OffsetDateTime reservationTime, Integer numberOfPeople) {
        // Implementa logica di controllo disponibilità
        if (numberOfPeople > restaurant.getCapacity()) {
            throw new ValidationException(ErrorConstants.LIMIT_MAX_PEOPLE_RESTURANT.name(), ErrorConstants.LIMIT_MAX_PEOPLE_RESTURANT.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InfoStatsDto getInfo(Long providerId) {
        RestaurantServiceEntity entity = restaurantServiceRepository.findByProviderIdForSingleService(providerId)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.RESTURANT_NOT_FOUND.name(),
                        ErrorConstants.RESTURANT_NOT_FOUND.getMessage()));

        InfoStatsDto infoStats = restaurantServiceRepository.getInfoStatsByProviderId(providerId);
        infoStats.setServiceId(entity.getId());

        return infoStats;
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantServiceDetailResponseDto getRestaurantServiceByIdAndProvider(Long serviceId, Long providerId) {
        String language = LocaleContextHolder.getLocale().getLanguage();
        RestaurantServiceEntity entity = restaurantServiceRepository.findDetailByIdAndProvider(serviceId, providerId, language)
                .orElseThrow(() -> new ValidationException(
                        ErrorConstants.RESTURANT_NOT_FOUND.name(),
                        "Ristorante non trovato o non appartiene al provider."));

        return restaurantServiceMapper.toProviderDetailDto(entity);
    }
}
