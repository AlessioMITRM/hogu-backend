package us.hogu.service.impl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import us.hogu.common.constants.ErrorConstants;
import us.hogu.configuration.security.JwtTokenProvider;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.controller.dto.request.OtpVerificationRequestDto;
import us.hogu.controller.dto.request.PasswordResetConfirmDto;
import us.hogu.controller.dto.request.PasswordResetDashboard;
import us.hogu.controller.dto.request.PasswordResetRequestDto;
import us.hogu.controller.dto.request.ProviderRegistrationRequestDto;
import us.hogu.controller.dto.request.UserDocumentRequestDto;
import us.hogu.controller.dto.request.UserLoginRequestDto;
import us.hogu.controller.dto.request.CustomerRegistrationRequestDto;
import us.hogu.controller.dto.request.UserUpdateRequestDto;
import us.hogu.controller.dto.response.AuthResponseDto;
import us.hogu.controller.dto.response.UserProfileResponseDto;
import us.hogu.controller.dto.response.UserResponseDto;
import us.hogu.converter.ServiceLocaleMapper;
import us.hogu.converter.UserMapper;
import us.hogu.exception.UserNotFoundException;
import us.hogu.model.User;
import us.hogu.model.ServiceLocale;
import us.hogu.model.UserDocument;
import us.hogu.model.UserOtp;
import us.hogu.model.UserServiceVerification;
import us.hogu.model.RestaurantServiceEntity;
import us.hogu.model.BnbServiceEntity;
import us.hogu.model.ClubServiceEntity;
import us.hogu.model.NccServiceEntity;
import us.hogu.model.LuggageServiceEntity;
import us.hogu.model.enums.ServiceType;
import us.hogu.model.enums.UserRole;
import us.hogu.model.enums.UserStatus;
import us.hogu.model.enums.VerificationStatusServiceEY;
import us.hogu.model.internal.ProviderServicesCheck;
import us.hogu.repository.jpa.UserDocumentJpa;
import us.hogu.repository.jpa.UserJpa;

import us.hogu.repository.jpa.UserOtpJpa;
import us.hogu.repository.jpa.UserServiceVerificationJpa;
import us.hogu.repository.jpa.RestaurantServiceJpa;
import us.hogu.repository.jpa.BnbServiceJpa;
import us.hogu.repository.jpa.ClubServiceJpa;
import us.hogu.repository.jpa.NccServiceJpa;
import us.hogu.repository.jpa.LuggageServiceJpa;
import us.hogu.repository.jpa.RestaurantBookingJpa;
import us.hogu.model.enums.BookingStatus;

import us.hogu.repository.projection.UserSummaryProjection;
import us.hogu.service.intefaces.EmailService;
import us.hogu.service.intefaces.FileService;
import us.hogu.service.intefaces.UserService;
import lombok.RequiredArgsConstructor;
import us.hogu.exception.ValidationException;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
        private final UserJpa userJpa;
        private final UserOtpJpa userOtpJpa;
        private final UserServiceVerificationJpa userServiceVerificationJpa;
        private final UserDocumentJpa userDocumentJpa;
        private final PasswordEncoder passwordEncoder;
        private final UserMapper userMapper;
        private final JwtTokenProvider jwtTokenProvider;
        private final EmailService emailService;
        private final ServiceLocaleMapper serviceLocaleMapper;
        private final RestaurantServiceJpa restaurantServiceJpa;
        private final BnbServiceJpa bnbServiceJpa;
        private final ClubServiceJpa clubServiceJpa;
        private final NccServiceJpa nccServiceJpa;
        private final LuggageServiceJpa luggageServiceJpa;
        private final RestaurantBookingJpa restaurantBookingRepository;
        private final FileService fileService;

        @Override
        @Transactional
        public AuthResponseDto customerRegistration(CustomerRegistrationRequestDto requestDto) {
                Optional<User> userAlreadyExist = userJpa.findByEmail(requestDto.getEmail());
                if (!userAlreadyExist.isEmpty()) {
                        otpAlreadyExistForEmail(userAlreadyExist.get(), OffsetDateTime.now());

                        throw new ValidationException(ErrorConstants.USER_EMAIL_ALREADY_EXISTS.name(),
                                        ErrorConstants.USER_EMAIL_ALREADY_EXISTS.getMessage());
                }

                User user = userMapper.toCustomerEntity(requestDto);
                user.setPasswordHash(passwordEncoder.encode(requestDto.getPassword()));
                user.setRole(UserRole.CUSTOMER);
                user.setCreationDate(OffsetDateTime.now());
                user.setStatus(UserStatus.PENDING);

                if (requestDto.getLocales() != null && !requestDto.getLocales().isEmpty()) {
                        List<ServiceLocale> locales = serviceLocaleMapper.mapRequestToEntity(requestDto.getLocales());
                        user.setServiceLocales(new ArrayList<>(locales));
                }

                User savedUser = userJpa.save(user);

                String otpCode = emailService.generateOtp();

                UserOtp userOtp = UserOtp.builder()
                                .otpCode(otpCode)
                                .expirationDate(OffsetDateTime.now().plusMinutes(5))
                                .user(savedUser)
                                .verified(false)
                                .build();

                userOtpJpa.save(userOtp);

                emailService.sendOtpEmail(requestDto.getEmail(), otpCode, requestDto.getLanguage());

                return AuthResponseDto.builder()
                                .token(null)
                                .user(userMapper.toResponseDto(savedUser))
                                .build();
        }

        @Override
        @Transactional
        public AuthResponseDto providerRegistration(ProviderRegistrationRequestDto request) throws Exception {
                Optional<User> userAlreadyExist = userJpa.findByEmail(request.getEmail());
                if (!userAlreadyExist.isEmpty()) {
                        otpAlreadyExistForEmail(userAlreadyExist.get(), OffsetDateTime.now());

                        throw new ValidationException(ErrorConstants.USER_EMAIL_ALREADY_EXISTS.name(),
                                        ErrorConstants.USER_EMAIL_ALREADY_EXISTS.getMessage());
                }

                // --- VALIDAZIONE DOCUMENTI ---
                if (request.getDocuments() == null || request.getDocuments().isEmpty()) {
                        throw new ValidationException("MISSING_DOCUMENTS",
                                        "È obbligatorio caricare almeno un documento per la registrazione Partner.");
                }

                for (UserDocumentRequestDto doc : request.getDocuments()) {
                        if (doc.getFile() == null || doc.getFile().isEmpty()) {
                                throw new ValidationException("INVALID_DOCUMENT",
                                                "Uno dei file caricati è vuoto o non valido.");
                        }
                        String contentType = doc.getFile().getContentType();
                        if (contentType == null || (!contentType.equals("application/pdf")
                                        && !contentType.startsWith("image/"))) {
                                throw new ValidationException("INVALID_FORMAT",
                                                "Formato file non supportato (" + contentType
                                                                + "). Carica solo PDF o Immagini.");
                        }
                }

                // --- SALVATAGGIO UTENTE ---
                User user = userMapper.toProviderEntity(request);
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                user.setRole(UserRole.PROVIDER);
                user.setCreationDate(OffsetDateTime.now());
                user.setStatus(UserStatus.PENDING);

                if (request.getLocales() != null && !request.getLocales().isEmpty()) {
                        List<ServiceLocale> locales = serviceLocaleMapper.mapRequestToEntity(request.getLocales());
                        user.setServiceLocales(new ArrayList<>(locales));
                }

                User savedUser = userJpa.save(user);

                // --- CREAZIONE ENTITA' SERVIZIO DEFAULT ---
                String defaultName = "Registrazione in corso...";
                String defaultDescription = request.getDescription() != null && !request.getDescription().isBlank()
                                ? request.getDescription()
                                : "Descrizione del servizio in aggiornamento.";

                switch (request.getServiceType()) {
                        case RESTAURANT:
                                RestaurantServiceEntity defaultRestaurant = RestaurantServiceEntity.builder()
                                                .user(savedUser)
                                                .name(defaultName)
                                                .description(defaultDescription)
                                                .locales(new ArrayList<>())
                                                .menu("")
                                                .capacity(0)
                                                .basePrice(request.getBasePrice() != null ? request.getBasePrice()
                                                                : java.math.BigDecimal.ZERO)
                                                .images(new ArrayList<>())
                                                .publicationStatus(false)
                                                .build();
                                restaurantServiceJpa.save(defaultRestaurant);
                                break;
                        case BNB:
                                BnbServiceEntity defaultBnb = BnbServiceEntity.builder()
                                                .user(savedUser)
                                                .name(defaultName)
                                                .description(defaultDescription)
                                                .defaultPricePerNight(request.getBasePrice() != null
                                                                ? request.getBasePrice()
                                                                : java.math.BigDecimal.ZERO)
                                                .totalRooms(0)
                                                .maxGuestsForRoom(0)
                                                .rooms(new ArrayList<>())
                                                .locales(new ArrayList<>())
                                                .images(new ArrayList<>())
                                                .publicationStatus(false)
                                                .build();
                                bnbServiceJpa.save(defaultBnb);
                                break;
                        case CLUB:
                                ClubServiceEntity defaultClub = ClubServiceEntity.builder()
                                                .user(savedUser)
                                                .name(defaultName)
                                                .description(defaultDescription)
                                                .locales(new ArrayList<>())
                                                .maxCapacity(request.getMaxCapacity() != null ? request.getMaxCapacity()
                                                                : 0L)
                                                .basePrice(request.getBasePrice() != null ? request.getBasePrice()
                                                                : java.math.BigDecimal.ZERO)
                                                .images(new ArrayList<>())
                                                .publicationStatus(false)
                                                .build();
                                clubServiceJpa.save(defaultClub);
                                break;
                        case NCC:
                                NccServiceEntity defaultNcc = NccServiceEntity.builder()
                                                .user(savedUser)
                                                .name(defaultName)
                                                .description(defaultDescription)
                                                .vehiclesAvailable(new ArrayList<>())
                                                .basePrice(request.getBasePrice() != null ? request.getBasePrice()
                                                                : java.math.BigDecimal.ZERO)
                                                .locales(new ArrayList<>())
                                                .images(new ArrayList<>())
                                                .publicationStatus(false)
                                                .build();
                                nccServiceJpa.save(defaultNcc);
                                break;
                        case LUGGAGE:
                                LuggageServiceEntity defaultLuggage = LuggageServiceEntity.builder()
                                                .user(savedUser)
                                                .name(defaultName)
                                                .description(defaultDescription)
                                                .locales(new ArrayList<>())
                                                .capacity(0)
                                                .basePrice(request.getBasePrice() != null ? request.getBasePrice()
                                                                : java.math.BigDecimal.ZERO)
                                                .images(new ArrayList<>())
                                                .publicationStatus(false)
                                                .build();
                                luggageServiceJpa.save(defaultLuggage);
                                break;
                        default:
                                break;
                }

                // --- SALVATAGGIO VERIFICA SERVIZIO ---
                UserServiceVerification userServiceVerification = UserServiceVerification.builder()
                                .user(savedUser)
                                .serviceType(request.getServiceType())
                                .licenseValid(false)
                                .vatValid(false)
                                .description(request.getDescription())
                                .verificationStatus(VerificationStatusServiceEY.PENDING)
                                .build();

                userServiceVerificationJpa.save(userServiceVerification);

                // --- SALVATAGGIO DOCUMENTI ---
                List<UserDocument> documents = new ArrayList<UserDocument>();
                for (UserDocumentRequestDto documentDto : request.getDocuments()) {
                        documents.add(
                                        UserDocument.builder()
                                                        .filename(documentDto.getFilename())
                                                        .fileData(documentDto.getFile().getBytes())
                                                        .approved(false)
                                                        .userServiceVerification(userServiceVerification)
                                                        .build());
                }
                userDocumentJpa.saveAll(documents);

                // --- GESTIONE OTP ---
                String otpCode = emailService.generateOtp();

                UserOtp userOtp = UserOtp.builder()
                                .otpCode(otpCode)
                                .expirationDate(OffsetDateTime.now().plusMinutes(5))
                                .user(savedUser)
                                .verified(false)
                                .build();

                userOtpJpa.save(userOtp);

                emailService.sendOtpEmail(request.getEmail(), otpCode, request.getLanguage());

                return AuthResponseDto.builder()
                                .token(null)
                                .user(userMapper.toResponseDto(savedUser))
                                .build();
        }

        @Override
        @Transactional
        public void resendOtpVerification(String email) {
                User user = userJpa.findByEmail(email)
                                .orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
                                                ErrorConstants.USER_NOT_FOUND.getMessage()));

                String otpCode = emailService.generateOtp();

                emailService.sendOtpEmail(email, otpCode, user.getLanguage());

                UserOtp userOtp = UserOtp.builder()
                                .otpCode(otpCode)
                                .expirationDate(OffsetDateTime.now().plusMinutes(5))
                                .user(user)
                                .verified(false)
                                .build();

                userOtpJpa.save(userOtp);
        }

        @Override
        @Transactional
        public AuthResponseDto verificateOtpCustomer(OtpVerificationRequestDto request) {
                User user = userJpa.findByEmail(request.getEmail())
                                .orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
                                                ErrorConstants.USER_NOT_FOUND.getMessage()));

                if (user.getRole() != UserRole.CUSTOMER) {
                        throw new ValidationException(ErrorConstants.USER_ROLE_INVALID.name(),
                                        ErrorConstants.USER_ROLE_INVALID.getMessage());
                }
                if (user.getStatus() != UserStatus.PENDING) {
                        throw new ValidationException(ErrorConstants.OTP_ALREADY_VALID.name(),
                                        ErrorConstants.OTP_ALREADY_VALID.getMessage());
                }

                UserOtp otp = userOtpJpa
                                .findFirstByUserAndVerifiedFalseAndExpirationDateAfterOrderByIdDesc(user,
                                                OffsetDateTime.now())
                                .orElseThrow(() -> new ValidationException(ErrorConstants.OTP_NOT_FOUND.name(),
                                                ErrorConstants.OTP_NOT_FOUND.getMessage()));

                if (!request.getOtpCode().equals(otp.getOtpCode())) {
                        throw new ValidationException(ErrorConstants.OTP_NOT_VALID.name(),
                                        ErrorConstants.OTP_NOT_VALID.getMessage());
                }

                otp.setVerified(true);

                userOtpJpa.save(otp);

                user.setStatus(UserStatus.ACTIVE);

                User savedUser = userJpa.save(user);

                String token = jwtTokenProvider.generateToken(savedUser);

                return AuthResponseDto.builder()
                                .token(token)
                                .user(userMapper.toResponseDto(savedUser))
                                .build();
        }

        @Override
        @Transactional
        public AuthResponseDto verificateOtpProvider(OtpVerificationRequestDto request) {
                User user = userJpa.findByEmail(request.getEmail())
                                .orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
                                                ErrorConstants.USER_NOT_FOUND.getMessage()));

                if (user.getRole() != UserRole.PROVIDER) {
                        throw new ValidationException(ErrorConstants.USER_ROLE_INVALID.name(),
                                        ErrorConstants.USER_ROLE_INVALID.getMessage());
                }
                if (user.getStatus() != UserStatus.PENDING) {
                        throw new ValidationException(ErrorConstants.OTP_ALREADY_VALID.name(),
                                        ErrorConstants.OTP_ALREADY_VALID.getMessage());
                }

                UserOtp otp = userOtpJpa
                                .findFirstByUserAndVerifiedFalseAndExpirationDateAfterOrderByIdDesc(user,
                                                OffsetDateTime.now())
                                .orElseThrow(() -> new ValidationException(ErrorConstants.OTP_NOT_FOUND.name(),
                                                ErrorConstants.OTP_NOT_FOUND.getMessage()));

                if (!request.getOtpCode().equals(otp.getOtpCode())) {
                        throw new ValidationException(ErrorConstants.OTP_NOT_VALID.name(),
                                        ErrorConstants.OTP_NOT_VALID.getMessage());
                }

                otp.setVerified(true);

                userOtpJpa.save(otp);

                user.setStatus(UserStatus.PENDING_ADMIN_APPROVAL);

                User savedUser = userJpa.save(user);

                return AuthResponseDto.builder()
                                .token(null)
                                .user(userMapper.toResponseDto(savedUser))
                                .build();
        }

        @Override
        @Transactional
        public AuthResponseDto login(UserLoginRequestDto requestDto) {
                User user = userJpa.findByEmail(requestDto.getEmail())
                                .orElseThrow(() -> new ValidationException(
                                                ErrorConstants.USER_CREDENTIAL_NOT_VALID.name(),
                                                ErrorConstants.USER_CREDENTIAL_NOT_VALID.getMessage()));

                if (!passwordEncoder.matches(requestDto.getPassword(), user.getPasswordHash())) {
                        throw new ValidationException(ErrorConstants.USER_CREDENTIAL_NOT_VALID.name(),
                                        ErrorConstants.USER_CREDENTIAL_NOT_VALID.getMessage());
                }
                if (user.getStatus() != UserStatus.ACTIVE) {
                        throw new ValidationException(ErrorConstants.ACCOUNT_NOT_ACTIVE.name(),
                                        ErrorConstants.ACCOUNT_NOT_ACTIVE.getMessage());
                }
                if (!user.getRole().name().equals(requestDto.getRole())) {
                        throw new ValidationException(ErrorConstants.USER_ROLE_INVALID.name(),
                                        ErrorConstants.USER_ROLE_INVALID.getMessage());
                }

                user.setLastLogin(OffsetDateTime.now());
                userJpa.save(user);

                String token = jwtTokenProvider.generateToken(user);

                ProviderServicesCheck servicesCheck = null;
                if (user.getRole() == UserRole.PROVIDER) {
                        servicesCheck = userJpa.checkProviderServices(user.getId());
                }

                return AuthResponseDto.builder()
                                .token(token)
                                .user(userMapper.toResponseDto(user))
                                .services(servicesCheck)
                                .build();
        }

        @Override
        public UserResponseDto getUserProfile(Long userId) {
                User user = userJpa.findById(userId)
                                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));
                return userMapper.toResponseDto(user);
        }

        @Override
        public List<UserSummaryProjection> getUsersByRole(UserRole role) {
                return userJpa.findByRole(role);
        }

        @Override
        @Transactional
        public UserProfileResponseDto updateUserProfile(Long userId, UserUpdateRequestDto request) {
                User user = userJpa.findById(userId)
                                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));

                if (user.getRole() == UserRole.CUSTOMER && request.getSurname().isBlank()) {
                        throw new ValidationException(ErrorConstants.GENERIC_ERROR.name(),
                                        ErrorConstants.GENERIC_ERROR.getMessage());
                }

                userMapper.updateEntityFromDto(request, user);

                if (request.getLocales() != null) {
                        List<ServiceLocale> locales = serviceLocaleMapper.mapRequestToEntity(request.getLocales());
                        if (user.getServiceLocales() == null) {
                                user.setServiceLocales(new ArrayList<>());
                        } else {
                                user.getServiceLocales().clear();
                        }
                        user.getServiceLocales().addAll(locales);
                }

                userJpa.save(user);

                return userMapper.toProfileDto(user);
        }

        @Override
        @Transactional
        public void requestPasswordReset(PasswordResetRequestDto requestDto) {
                // 1. Cerchiamo l'utente
                User user = userJpa.findByEmail(requestDto.getEmail())
                                .orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
                                                ErrorConstants.USER_NOT_FOUND.getMessage()));

                if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.DEACTIVATED) {
                        throw new ValidationException("USER_BANNED",
                                        "L'utente è bloccato e non può resettare la password");
                }

                String otpCode = emailService.generateOtp();

                UserOtp userOtp = UserOtp.builder()
                                .otpCode(otpCode)
                                .expirationDate(OffsetDateTime.now().plusMinutes(5)) // Validità 5 minuti
                                .user(user)
                                .verified(false)
                                .build();

                userOtpJpa.save(userOtp);

                emailService.sendOtpEmailForResetPassword(user.getEmail(), otpCode, user.getLanguage());
        }

        @Override
        @Transactional
        public void passwordResetDashboard(UserAccount userAccount, PasswordResetDashboard requestDto) {
                // 1. Cerchiamo l'utente
                User user = userJpa.findById(userAccount.getAccountId())
                                .orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
                                                ErrorConstants.USER_NOT_FOUND.getMessage()));

                if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.DEACTIVATED) {
                        throw new ValidationException(ErrorConstants.USER_NOT_AUTHORIZED.name(),
                                        ErrorConstants.USER_NOT_AUTHORIZED.getMessage());
                }

                user.setPasswordHash(passwordEncoder.encode(requestDto.getPassword()));

                userJpa.save(user);
        }

        @Override
        @Transactional
        public void confirmPasswordReset(PasswordResetConfirmDto confirmDto) {
                User user = userJpa.findByEmail(confirmDto.getEmail())
                                .orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(),
                                                ErrorConstants.USER_NOT_FOUND.getMessage()));

                UserOtp otp = userOtpJpa
                                .findFirstByUserAndVerifiedFalseAndExpirationDateAfterOrderByIdDesc(user,
                                                OffsetDateTime.now())
                                .orElseThrow(() -> new ValidationException(ErrorConstants.OTP_NOT_FOUND.name(),
                                                ErrorConstants.OTP_NOT_FOUND.getMessage()));

                if (!confirmDto.getOtpCode().equals(otp.getOtpCode())) {
                        throw new ValidationException(ErrorConstants.OTP_NOT_VALID.name(),
                                        ErrorConstants.OTP_NOT_VALID.getMessage());
                }

                // Marcatura OTP come verificato (per non poterlo riusare)
                otp.setVerified(true);
                userOtpJpa.save(otp);

                user.setPasswordHash(passwordEncoder.encode(confirmDto.getNewPassword()));

                userJpa.save(user);
        }

        private void otpAlreadyExistForEmail(User user, OffsetDateTime expiredOtp) {
                Optional<UserOtp> otp = userOtpJpa.findFirstByUserAndVerifiedFalseAndExpirationDateAfterOrderByIdDesc(
                                user,
                                expiredOtp);

                if (!otp.isEmpty()) {
                        throw new ValidationException(ErrorConstants.OTP_ALREADY_EXISTS.name(),
                                        ErrorConstants.OTP_ALREADY_EXISTS.getMessage());
                }
        }

        @Override
        @Transactional
        public void deleteCustomerAccount(Long accountId) {
                User user = userJpa.findById(accountId)
                                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));

                if (user.getRole() != UserRole.CUSTOMER) {
                        throw new ValidationException(ErrorConstants.USER_ROLE_INVALID.name(),
                                        "Solo i clienti possono eliminare il proprio account da questa funzione.");
                }

                user.setName("Utente");
                user.setSurname("Cancellato");
                String originalEmail = user.getEmail();
                user.setEmail("deleted_" + user.getId() + "@deleted.hogu.us");
                user.setFiscalCode(null);

                // Un-set the password to prevent login.
                // Use a random UUID because the password hash could have constraints on
                // size/format.
                user.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));

                user.setStatus(UserStatus.DELETED);

                emailService.sendEmailForAccountDeletion(originalEmail, user.getLanguage());

                userJpa.save(user);
        }

        @Override
        @Transactional
        public void deleteProviderAccount(Long accountId) {
                User user = userJpa.findById(accountId)
                                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));

                if (user.getRole() != UserRole.PROVIDER) {
                        throw new ValidationException(ErrorConstants.USER_ROLE_INVALID.name(),
                                        "Solo i partner possono eliminare il proprio account da questa funzione.");
                }

                // Controllo commissioni in sospeso (Prenotazioni COMPLETED ma non
                // COMMISSION_PAID)
                Long completedPeople = restaurantBookingRepository.sumCompletedPeopleByProviderId(accountId);
                if (completedPeople != null && completedPeople > 0) {
                        throw new ValidationException("PENDING_COMMISSIONS",
                                        "Impossibile eliminare l'account: ci sono commissioni in sospeso da pagare per il ristorante.");
                }

                // Controllo prenotazioni in attesa di conferma
                Long pendingBookings = restaurantBookingRepository.countByProviderIdAndStatus(accountId,
                                BookingStatus.WAITING_PROVIDER_CONFIRMATION);
                if (pendingBookings != null && pendingBookings > 0) {
                        throw new ValidationException("PENDING_BOOKINGS",
                                        "Impossibile eliminare l'account: ci sono prenotazioni in attesa di conferma.");
                }

                user.setName("Utente");
                user.setSurname("Cancellato");
                String originalEmail = user.getEmail();
                user.setEmail("deleted_" + user.getId() + "@deleted.hogu.us");
                user.setFiscalCode(null);

                // Un-set the password to prevent login.
                user.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));

                user.setStatus(UserStatus.DELETED);

                // Invalidazione Servizi e Documenti
                if (user.getServiceVerifications() != null) {
                        for (UserServiceVerification verification : user.getServiceVerifications()) {
                                verification.setVerificationStatus(VerificationStatusServiceEY.DELETED);
                                verification.setLicenseValid(false);
                                verification.setVatValid(false);

                                if (verification.getUserDocuments() != null) {
                                        for (UserDocument document : verification.getUserDocuments()) {
                                                document.setApproved(false);
                                        }
                                }
                        }
                }

                emailService.sendEmailForAccountDeletion(originalEmail, user.getLanguage());

                userJpa.save(user);

                // Disattivazione servizi BnB e relative camere
                List<BnbServiceEntity> bnbServices = bnbServiceJpa.findByUser(user);
                for (BnbServiceEntity bnb : bnbServices) {
                        bnb.setPublicationStatus(false);
                        if (bnb.getRooms() != null) {
                                for (us.hogu.model.BnbRoom room : bnb.getRooms()) {
                                        room.setPublicationStatus(false);
                                }
                        }
                        fileService.deleteServiceImages(bnb.getId(), ServiceType.BNB);
                }
                if (!bnbServices.isEmpty()) {
                        bnbServiceJpa.saveAll(bnbServices);
                }

                // Disattivazione servizi Ristorante
                List<RestaurantServiceEntity> restaurantServices = restaurantServiceJpa.findByUser(user);
                for (RestaurantServiceEntity restaurant : restaurantServices) {
                        restaurant.setPublicationStatus(false);
                        fileService.deleteServiceImages(restaurant.getId(), ServiceType.RESTAURANT);
                }
                if (!restaurantServices.isEmpty()) {
                        restaurantServiceJpa.saveAll(restaurantServices);
                }

                // Disattivazione servizi Club e relativi eventi
                List<ClubServiceEntity> clubServices = clubServiceJpa.findByUser(user);
                for (ClubServiceEntity club : clubServices) {
                        club.setPublicationStatus(false);
                        if (club.getEvents() != null) {
                                for (us.hogu.model.EventClubServiceEntity event : club.getEvents()) {
                                        event.setIsActive(false);
                                        event.setDeleted(true);
                                }
                        }
                        fileService.deleteServiceImages(club.getId(), ServiceType.CLUB);
                }
                if (!clubServices.isEmpty()) {
                        clubServiceJpa.saveAll(clubServices);
                }

                // Disattivazione servizi NCC
                List<NccServiceEntity> nccServices = nccServiceJpa.findByUser(user);
                for (NccServiceEntity ncc : nccServices) {
                        ncc.setPublicationStatus(false);
                        fileService.deleteServiceImages(ncc.getId(), ServiceType.NCC);
                }
                if (!nccServices.isEmpty()) {
                        nccServiceJpa.saveAll(nccServices);
                }

                // Disattivazione servizi Luggage
                List<LuggageServiceEntity> luggageServices = luggageServiceJpa.findByUser(user);
                for (LuggageServiceEntity luggage : luggageServices) {
                        luggage.setPublicationStatus(false);
                        fileService.deleteServiceImages(luggage.getId(), ServiceType.LUGGAGE);
                }
                if (!luggageServices.isEmpty()) {
                        luggageServiceJpa.saveAll(luggageServices);
                }
        }
}
