package us.hogu.service.impl;

import java.io.IOException;
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
import us.hogu.converter.UserMapper;
import us.hogu.exception.UserNotFoundException;
import us.hogu.model.User;
import us.hogu.model.UserDocument;
import us.hogu.model.UserOtp;
import us.hogu.model.UserServiceVerification;
import us.hogu.model.enums.UserRole;
import us.hogu.model.enums.UserStatus;
import us.hogu.model.enums.VerificationStatusServiceEY;
import us.hogu.model.internal.ProviderServicesCheck;
import us.hogu.repository.jpa.UserDocumentJpa;
import us.hogu.repository.jpa.UserJpa;
import us.hogu.repository.jpa.UserOtpJpa;
import us.hogu.repository.jpa.UserServiceVerificationJpa;
import us.hogu.repository.projection.UserProfileProjection;
import us.hogu.repository.projection.UserSummaryProjection;
import us.hogu.service.intefaces.EmailService;
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
    private final EmailService emailService ;
    
    
    @Override
    @Transactional
    public AuthResponseDto customerRegistration(CustomerRegistrationRequestDto requestDto) {
    	Optional<User> userAlreadyExist = userJpa.findByEmail(requestDto.getEmail());
        if (!userAlreadyExist.isEmpty()) 
        {
        	otpAlreadyExistForEmail(userAlreadyExist.get(), OffsetDateTime.now());
        	
            throw new ValidationException(ErrorConstants.USER_EMAIL_ALREADY_EXISTS.name(), ErrorConstants.USER_EMAIL_ALREADY_EXISTS.getMessage());
        }
        
        User user = userMapper.toCustomerEntity(requestDto);
        user.setPasswordHash(passwordEncoder.encode(requestDto.getPassword()));
        user.setRole(UserRole.CUSTOMER);
        user.setCreationDate(OffsetDateTime.now());
        user.setStatus(UserStatus.PENDING);
        
        User savedUser = userJpa.save(user);

        String otpCode = emailService.generateOtp();
                
        UserOtp userOtp = UserOtp.builder()
        		.otpCode(otpCode)
        		.expirationDate(OffsetDateTime.now().plusMinutes(5))
        		.user(savedUser)
        		.verified(false)
        		.build();
        
        userOtpJpa.save(userOtp);
        
        emailService.sendOtpEmail(requestDto.getEmail(), otpCode);
        
        return AuthResponseDto.builder()
            .token(null)
            .user(userMapper.toResponseDto(savedUser))
            .build();
    }
    
    @Override
    @Transactional
    public AuthResponseDto providerRegistration(ProviderRegistrationRequestDto request) throws Exception {
    	Optional<User> userAlreadyExist = userJpa.findByEmail(request.getEmail());
        if (!userAlreadyExist.isEmpty()) 
        {
        	otpAlreadyExistForEmail(userAlreadyExist.get(), OffsetDateTime.now());
        	
            throw new ValidationException(ErrorConstants.USER_EMAIL_ALREADY_EXISTS.name(), ErrorConstants.USER_EMAIL_ALREADY_EXISTS.getMessage());
        }
        
        User user = userMapper.toProviderEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.PROVIDER);
        user.setStatus(UserStatus.PENDING);
        
        User savedUser = userJpa.save(user);

        UserServiceVerification userServiceVerification = UserServiceVerification.builder()
        		.user(savedUser)
        		.serviceType(request.getServiceType())
        		.licenseValid(false)
        		.vatValid(false)
        		.verificationStatus(VerificationStatusServiceEY.PENDING)
        		.build();
        
        userServiceVerificationJpa.save(userServiceVerification);
        
        if(request.getDocuments() != null && !request.getDocuments().isEmpty()) {
	        List<UserDocument> documents = new ArrayList<UserDocument>();
	        for(UserDocumentRequestDto documentDto : request.getDocuments()) {
	        	documents.add(
		        	UserDocument.builder()
		        		.filename(documentDto.getFilename())
		        		.fileData(documentDto.getFile().getBytes())
		        		.approved(false)
		        		.userServiceVerification(userServiceVerification)
		        		.build()
	        	);
	        }
	        userDocumentJpa.saveAll(documents);
        }
        
        String otpCode = emailService.generateOtp();
                
        UserOtp userOtp = UserOtp.builder()
        		.otpCode(otpCode)
        		.expirationDate(OffsetDateTime.now().plusMinutes(5))
        		.user(savedUser)
        		.verified(false)
        		.build();
        
        userOtpJpa.save(userOtp);
        
        emailService.sendOtpEmail(request.getEmail(), otpCode);
        
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
        
        emailService.sendOtpEmail(email, otpCode);
        
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
    public AuthResponseDto verificateOtpCustomer(OtpVerificationRequestDto request) 
    {        
        User user = userJpa.findByEmail(request.getEmail())
        		.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(), 
            			ErrorConstants.USER_NOT_FOUND.getMessage()));
        
        if(user.getRole() != UserRole.CUSTOMER) {
        	throw new ValidationException(ErrorConstants.USER_ROLE_INVALID.name(), 
        			ErrorConstants.USER_ROLE_INVALID.getMessage());
        }
        if(user.getStatus() != UserStatus.PENDING) {
        	throw new ValidationException(ErrorConstants.OTP_ALREADY_VALID.name(), 
        			ErrorConstants.OTP_ALREADY_VALID.getMessage());
        }

        UserOtp otp = userOtpJpa.findFirstByUserAndVerifiedFalseAndExpirationDateAfterOrderByIdDesc(user, OffsetDateTime.now())
                .orElseThrow(() -> new ValidationException(ErrorConstants.OTP_NOT_FOUND.name(),
                    ErrorConstants.OTP_NOT_FOUND.getMessage()
                ));
        
        if(!request.getOtpCode().equals(otp.getOtpCode())) {
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
    public AuthResponseDto verificateOtpProvider(OtpVerificationRequestDto request) 
    {        
        User user = userJpa.findByEmail(request.getEmail())
        		.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(), 
            			ErrorConstants.USER_NOT_FOUND.getMessage()));
        
        if(user.getRole() != UserRole.PROVIDER) {
        	throw new ValidationException(ErrorConstants.USER_ROLE_INVALID.name(), 
        			ErrorConstants.USER_ROLE_INVALID.getMessage());
        }
        if(user.getStatus() != UserStatus.PENDING) {
           	throw new ValidationException(ErrorConstants.OTP_ALREADY_VALID.name(), 
        			ErrorConstants.OTP_ALREADY_VALID.getMessage());
        }

        UserOtp otp = userOtpJpa.findFirstByUserAndVerifiedFalseAndExpirationDateAfterOrderByIdDesc(user, OffsetDateTime.now())
                .orElseThrow(() -> new ValidationException(ErrorConstants.OTP_NOT_FOUND.name(),
                    ErrorConstants.OTP_NOT_FOUND.getMessage()
                ));
        
        if(!request.getOtpCode().equals(otp.getOtpCode())) {
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
            .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));
        
		if(!passwordEncoder.matches(requestDto.getPassword(), user.getPasswordHash())) {
			throw new ValidationException(ErrorConstants.USER_CREDENTIAL_NOT_VALID.name(),
					ErrorConstants.USER_CREDENTIAL_NOT_VALID.getMessage());
		}
		if(user.getStatus() != UserStatus.ACTIVE) {
			throw new ValidationException(ErrorConstants.ACCOUNT_NOT_ACTIVE.name(),
					ErrorConstants.ACCOUNT_NOT_ACTIVE.getMessage());
		}
		if(!user.getRole().name().equals(requestDto.getRole())) {
			throw new ValidationException(ErrorConstants.USER_ROLE_INVALID.name(),
					ErrorConstants.USER_ROLE_INVALID.getMessage());
		}
        
        user.setLastLogin(OffsetDateTime.now());
        userJpa.save(user);
        
        String token = jwtTokenProvider.generateToken(user);
        
        ProviderServicesCheck servicesCheck = null;
        if(user.getRole() == UserRole.PROVIDER) {
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
    public UserProfileResponseDto updateUserProfile(Long userId, UserUpdateRequestDto requestDto) {
        User user = userJpa.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));
        
        userMapper.updateEntityFromDto(requestDto, user);
        
        userJpa.save(user);
        
        return UserProfileResponseDto.builder()
	        .name(user.getName())
	        .surname(user.getSurname())
	        .email(user.getEmail())
	        .role(user.getRole())
	        .lastLogin(user.getLastLogin())
	        .creationDate(user.getCreationDate())
	        .build();        
    }
    
    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequestDto requestDto) {
        // 1. Cerchiamo l'utente
        User user = userJpa.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(), 
                        ErrorConstants.USER_NOT_FOUND.getMessage()));

        if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.DEACTIVATED) {
             throw new ValidationException("USER_BANNED", "L'utente è bloccato e non può resettare la password");
        }

        String otpCode = emailService.generateOtp();

        UserOtp userOtp = UserOtp.builder()
                .otpCode(otpCode)
                .expirationDate(OffsetDateTime.now().plusMinutes(5)) // Validità 5 minuti
                .user(user)
                .verified(false)
                .build();

        userOtpJpa.save(userOtp);

        emailService.sendOtpEmailForResetPassword(user.getEmail(), otpCode);
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

        UserOtp otp = userOtpJpa.findFirstByUserAndVerifiedFalseAndExpirationDateAfterOrderByIdDesc(user, OffsetDateTime.now())
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
    
    private void setStatusAccountForVerificateOtp(User user, UserRole userRole) 
    {
        if(userRole == UserRole.CUSTOMER)
        	user.setStatus(UserStatus.ACTIVE);
        else if(userRole == UserRole.PROVIDER)
    		user.setStatus(UserStatus.PENDING_ADMIN_APPROVAL);
        else
        	throw new ValidationException(ErrorConstants.USER_ROLE_INVALID.name(), 
        			ErrorConstants.USER_ROLE_INVALID.getMessage());
    }
    
    private void otpAlreadyExistForEmail(User user, OffsetDateTime expiredOtp) 
    {
        Optional<UserOtp> otp = userOtpJpa.findFirstByUserAndVerifiedFalseAndExpirationDateAfterOrderByIdDesc(user, expiredOtp);
        
        if(!otp.isEmpty()) {
            throw new ValidationException(ErrorConstants.OTP_ALREADY_EXISTS.name(), 
            		ErrorConstants.OTP_ALREADY_EXISTS.getMessage());
        }
    }
}
