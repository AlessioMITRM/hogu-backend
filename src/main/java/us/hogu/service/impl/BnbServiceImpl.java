package us.hogu.service.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import us.hogu.common.constants.ErrorConstants;
import us.hogu.common.util.ImageUtils;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.BnbRoomPriceRequestDto;
import us.hogu.controller.dto.request.BnbRoomRequestDto;
import us.hogu.controller.dto.request.BnbSearchRequestDto;
import us.hogu.controller.dto.request.BnbServiceRequestDto;
import us.hogu.controller.dto.response.BnbBookingResponseDto;
import us.hogu.controller.dto.response.BnbRoomResponseDto;
import us.hogu.controller.dto.response.BnbSearchResponseDto;
import us.hogu.controller.dto.response.BnbSearchResponseDto.BnbSearchResultDto;
import us.hogu.controller.dto.response.BnbServiceResponseDto;
import us.hogu.converter.BnbMapper;
import us.hogu.converter.ServiceLocaleMapper;
import us.hogu.exception.ResourceNotFoundException;
import us.hogu.exception.UserNotFoundException;
import us.hogu.exception.ValidationException;
import us.hogu.model.BnbBooking;
import us.hogu.model.BnbRoom;
import us.hogu.model.BnbRoomPriceCalendar;
import us.hogu.model.BnbServiceEntity;
import us.hogu.model.User;
import us.hogu.model.enums.ServiceType;
import us.hogu.repository.jdbc.BnbRoomJdbc;
import us.hogu.repository.jpa.BnbBookingJpa;
import us.hogu.repository.jpa.BnbRoomJpa;
import us.hogu.repository.jpa.BnbRoomPriceCalendarJpa;
import us.hogu.repository.jpa.BnbServiceJpa;
import us.hogu.repository.jpa.UserJpa;
import us.hogu.service.intefaces.BnbService;
import us.hogu.service.intefaces.FileService;

@RequiredArgsConstructor
@Service
public class BnbServiceImpl implements BnbService {
    private final UserJpa userJpa;
    private final BnbServiceJpa bnbServiceJpa;
    private final BnbRoomJpa bnbRoomJpa;
    private final BnbRoomPriceCalendarJpa bnbRoomPriceCalendarJpa;
    private final BnbBookingJpa bnbBookingJpa;
    private final BnbRoomJdbc bnbRoomJdbc;
    private final FileService fileService;
    private final ServiceLocaleMapper localeMapper;
    private final BnbMapper bnbMapper;


    // 🔹 RICERCA CAMERE B&B
    @Override
    @Transactional(readOnly = true)
    public BnbSearchResponseDto searchBnbRooms(BnbSearchRequestDto searchRequest) {
        
        // Validazione date
        if (searchRequest.getCheckIn() != null && searchRequest.getCheckOut() != null 
            && !searchRequest.isValidDateRange()) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }

        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize());

        Locale locale = LocaleContextHolder.getLocale();
        Page<BnbSearchResultDto> searchResults = bnbRoomJdbc.searchNative(searchRequest, 
        		pageable, locale.getLanguage());

        return BnbSearchResponseDto.builder()
            .content(searchResults.getContent())
            .totalPages(searchResults.getTotalPages())
            .totalElements(searchResults.getTotalElements())
            .currentPage(searchResults.getNumber())
            .pageSize(searchResults.getSize())
            .build();
    }

    // 🔹 OTTIENI TUTTI I SERVIZI PUBBLICATI
    @Override
    public List<BnbServiceResponseDto> getAllPublishedBnbServices() {
        return bnbServiceJpa.findByPublicationStatus(true)
                .stream()
                .map(bnbMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 🔹 OTTIENI UN SINGOLO SERVIZIO
    @Override
    public Optional<BnbServiceResponseDto> getBnbServiceById(Long id) {
        return bnbServiceJpa.findById(id).map(bnbMapper::toResponse);
    }
    
    // 🔹 OTTIENI LE CAMERE DI UN SERVIZIO
    @Override
	public Page<BnbRoomResponseDto> getRoomsForService(Long bnbServiceId, Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}
    
    // 🔹 OTTIENI UNA CAMERE DI UN SERVIZIO
    @Override
	public BnbRoomResponseDto getRoomById(Long id, LocalDate checkIn, LocalDate checkOut) 
    {
        Locale locale = LocaleContextHolder.getLocale();
    	
        return bnbRoomJdbc.getRoomById(id, checkIn, checkOut, locale.getLanguage());
	}
	
    // 🔹 LISTA PRENOTAZIONI UTENTE
    @Override
    public Page<BnbBookingResponseDto> getBookingsForUser(Long userId, Pageable pageable) {
       /* return bnbBookingJpa.findByUserId(userId)
                .stream()
                .map(bnbMapper::toBookingResponse)
                .collect(Collectors.toList()); */
    	return null;
    }

    // 🔹 AGGIUNGI CAMERA A SERVIZIO
    //@Override
    @Transactional
    public BnbRoomResponseDto addRoomToService(Long bnbServiceId, BnbRoomRequestDto dto) {
        BnbServiceEntity service = bnbServiceJpa.findById(bnbServiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Servizio B&B non trovato"));

        BnbRoom room = bnbMapper.toRoomEntity(dto);
        room.setBnbService(service);
        room.setAvailable(true);

        return bnbMapper.toRoomResponse(bnbRoomJpa.save(room));
    }
    
    //@Override
    @Transactional
    public BnbServiceResponseDto createBnbService(BnbServiceRequestDto dto, Long providerId) {
        User provider = userJpa.findById(providerId)
                .orElseThrow(() -> new UserNotFoundException("Fornitore non trovato"));

        BnbServiceEntity entity = bnbMapper.toEntity(dto);
        entity.setUser(provider);
        entity.setPublicationStatus(false);

        return bnbMapper.toResponse(bnbServiceJpa.save(entity));
    }

    // 🔹 AGGIUNGI PERIODO DI PREZZO AD UNA CAMERA
    //@Override
    @Transactional
    public void addRoomPrice(Long roomId, BnbRoomPriceRequestDto dto) {
        BnbRoom room = bnbRoomJpa.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera non trovata"));

        BnbRoomPriceCalendar price = bnbMapper.toRoomPriceEntity(dto);
        price.setRoom(room);
        bnbRoomPriceCalendarJpa.save(price);
    }

    // 🔹 PRENOTAZIONE
    @Override
    @Transactional
    public BnbBookingResponseDto createBooking(Long userId, Long roomId, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        User user = userJpa.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));
        BnbRoom room = bnbRoomJpa.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera non trovata"));
        BnbServiceEntity service = room.getBnbService();

        double totalAmount = calculateTotalAmount(room, checkIn, checkOut);

        BnbBooking booking = BnbBooking.builder()
                .user(user)
                .bnbService(service)
                .room(room)
                .serviceType(service.getLocales().get(0).getServiceType())
                .serviceId(service.getId())
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .numberOfGuests(guests)
                .status(us.hogu.model.enums.BookingStatus.PENDING)
                .totalAmount(totalAmount)
                .build();

        return bnbMapper.toBookingResponse(bnbBookingJpa.save(booking));
    }
    
	@Override
	public void addRoomPrice(UserAccount userAccount, Long roomId, BnbRoomPriceRequestDto dto) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public BnbRoomResponseDto addRoomToService(UserAccount userAccount, Long bnbServiceId, BnbRoomRequestDto dto,
			List<MultipartFile> images) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<BnbBookingResponseDto> getBookingsForProvider(UserAccount userAccount, Long id, Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BnbServiceResponseDto createBnbService(UserAccount userAccount, @Valid BnbServiceRequestDto request,
			List<MultipartFile> images) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object updateBnbService(Long id, UserAccount userAccount, @Valid BnbServiceRequestDto request,
			List<MultipartFile> images) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object updateRoom(UserAccount userAccount, Long serviceId, Long roomId, @Valid BnbRoomRequestDto request,
			List<MultipartFile> images) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Page<BnbServiceResponseDto> getAllBnbServicesByProvider(long accountId, Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

    private double calculateTotalAmount(BnbRoom room, LocalDate checkIn, LocalDate checkOut) {
        List<BnbRoomPriceCalendar> calendar = bnbRoomPriceCalendarJpa.findByRoomId(room.getId());
        double total = 0;
        LocalDate date = checkIn;

        while (date.isBefore(checkOut)) {
            double priceForDay = room.getBasePricePerNight();

            for (BnbRoomPriceCalendar p : calendar) {
                if (!date.isBefore(p.getStartDate()) && !date.isAfter(p.getEndDate())) {
                    priceForDay = p.getPricePerNight();
                    break;
                }
            }

            total += priceForDay;
            date = date.plusDays(1);
        }

        return total;
    }
    

    // Remaining existing methods...
}
