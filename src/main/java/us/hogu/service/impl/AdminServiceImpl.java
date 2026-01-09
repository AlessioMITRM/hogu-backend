package us.hogu.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import us.hogu.common.constants.ErrorConstants;
import us.hogu.controller.dto.response.PendingUserResponseDto;
import us.hogu.controller.dto.response.UserDocumentResponseDto;
import us.hogu.controller.dto.response.UserServiceVerificationResponseDto;
import us.hogu.exception.ValidationException;
import us.hogu.model.BnbServiceEntity;
import us.hogu.model.ClubServiceEntity;
import us.hogu.model.LuggageServiceEntity;
import us.hogu.model.NccServiceEntity;
import us.hogu.model.RestaurantServiceEntity;
import us.hogu.model.User;
import us.hogu.model.UserDocument;
import us.hogu.model.UserOtp;
import us.hogu.model.UserServiceVerification;
import us.hogu.model.enums.UserStatus;
import us.hogu.model.enums.VerificationStatusServiceEY;
import us.hogu.repository.jpa.BnbBookingJpa;
import us.hogu.repository.jpa.BnbServiceJpa;
import us.hogu.repository.jpa.ClubServiceJpa;
import us.hogu.repository.jpa.LuggageServiceJpa;
import us.hogu.repository.jpa.NccServiceJpa;
import us.hogu.repository.jpa.RestaurantServiceJpa;
import us.hogu.repository.jpa.UserDocumentJpa;
import us.hogu.repository.jpa.UserJpa;
import us.hogu.repository.jpa.UserOtpJpa;
import us.hogu.repository.jpa.UserServiceVerificationJpa;
import us.hogu.repository.projection.UserDocumentForGetAllProjection;
import us.hogu.service.intefaces.AdminService;
import us.hogu.service.intefaces.EmailService;

@RequiredArgsConstructor
@Service
public class AdminServiceImpl implements AdminService {
    private final UserJpa userJpa;
    private final UserOtpJpa userOtpJpa;
    private final UserServiceVerificationJpa userServiceVerificationJpa;
    private final UserDocumentJpa userDocumentJpa;
    private final NccServiceJpa nccServiceJpa;
    private final RestaurantServiceJpa restaurantServiceJpa;
    private final ClubServiceJpa clubServiceJpa;
    private final LuggageServiceJpa luggageServiceJpa;
    private final BnbServiceJpa bnbServiceJpa;
    private final BnbBookingJpa bnbBookingJpa;
    private final EmailService emailService;
	
	
    @Override
    @Transactional(readOnly = true)
    public List<PendingUserResponseDto> getProviderAccountsPending() {
    	List<User> users = userJpa.findByStatus(UserStatus.PENDING_ADMIN_APPROVAL);
    	
    	List<PendingUserResponseDto> pendingUsers = new ArrayList<PendingUserResponseDto>();
    	for(User user : users) {
    		PendingUserResponseDto pendingUser = new PendingUserResponseDto();
    		pendingUser.setIdUser(user.getId());
    		pendingUser.setName(user.getName());
    		
    		List<UserServiceVerification> userServiceVerifications = 
	    			userServiceVerificationJpa.findByVerificationStatusAndUser(VerificationStatusServiceEY.PENDING, user);
    		
    		List<UserServiceVerificationResponseDto> userServiceVerificationsDto = 
    				new ArrayList<UserServiceVerificationResponseDto>();
    		
        	for(UserServiceVerification userServiceVerification : userServiceVerifications) {
        		UserServiceVerificationResponseDto userServiceVerificationDto = new UserServiceVerificationResponseDto();
        		userServiceVerificationDto.setId(userServiceVerification.getId());
        		userServiceVerificationDto.setServiceType(userServiceVerificationDto.getServiceType());
        		userServiceVerificationDto.setLicenseValid(userServiceVerification.isLicenseValid());
        		userServiceVerificationDto.setVatValid(userServiceVerification.isVatValid());
        		userServiceVerificationDto.setVerificationStatus(userServiceVerification.getVerificationStatus());
        		userServiceVerificationDto.setLastUpdateDate(userServiceVerification.getLastUpdateDate());
        		
    			List<UserDocumentForGetAllProjection> userDocuments = userDocumentJpa.findForGetAll(userServiceVerification);
    			List<UserDocumentResponseDto> userDocumentsResponseDto = new ArrayList<UserDocumentResponseDto>();
    			for(UserDocumentForGetAllProjection userDocument : userDocuments) {
        			UserDocumentResponseDto userDocumentResponseDto = new UserDocumentResponseDto();
        			userDocumentResponseDto.setId(userDocument.getId());
        			userDocumentResponseDto.setFilename(userDocument.getFilename());
        			userDocumentResponseDto.setApproved(userDocument.isApproved());
        			
        			userDocumentsResponseDto.add(userDocumentResponseDto);
        		}
    			userServiceVerificationDto.setDocuments(userDocumentsResponseDto);	
        		
        		userServiceVerificationsDto.add(userServiceVerificationDto);
        	}
        	
        	pendingUser.setUserServiceverifications(userServiceVerificationsDto);
        	
        	pendingUsers.add(pendingUser);
    	}    
    	        
    	return pendingUsers;
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserDocumentResponseDto getFileUserDocument(Long idDocument) {
		UserDocument userDocument = userDocumentJpa.findById(idDocument)
				.orElseThrow(() -> new ValidationException(ErrorConstants.DOCUMENT_NOT_FOUND.name(), 
            			ErrorConstants.DOCUMENT_NOT_FOUND.getMessage()));

		UserDocumentResponseDto userDocumentResponseDto = new UserDocumentResponseDto();
		userDocumentResponseDto.setId(userDocument.getId());
		userDocumentResponseDto.setFilename(userDocument.getFilename());
		userDocumentResponseDto.setFileData(userDocument.getFileData());
		userDocumentResponseDto.setApproved(userDocument.isApproved());
		
		return userDocumentResponseDto;
    }
    
    @Override
    @Transactional
    public void approveProviderAccount(Long idUser) {
    	User user = userJpa.findById(idUser)
    			.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(), 
            			ErrorConstants.USER_NOT_FOUND.getMessage()));
    	
    	user.setStatus(UserStatus.ACTIVE);
    	
    	userJpa.save(user);
    	
   		List<UserServiceVerification> userServiceVerifications = 
    			userServiceVerificationJpa.findByVerificationStatusAndUser(VerificationStatusServiceEY.PENDING, user);
   		
   		for(UserServiceVerification userServiceVerification : userServiceVerifications) {
   			userServiceVerification.setVerificationStatus(VerificationStatusServiceEY.ACTIVE);
   			
   			List<UserDocument> userDocuments = userDocumentJpa.findByUserServiceVerification(userServiceVerification);
   			for(UserDocument userDocument : userDocuments) {
   				userDocument.setApproved(true);
    			
    			userDocumentJpa.save(userDocument);
    		}
   		}
   		
   		userServiceVerificationJpa.saveAll(userServiceVerifications);     		
    }
    
    @Override
    @Transactional
    public void rejectProviderAccount(Long idUser, String motivation) {
    	User user = userJpa.findById(idUser) 
    			.orElseThrow(() -> new ValidationException(ErrorConstants.USER_NOT_FOUND.name(), 
            			ErrorConstants.USER_NOT_FOUND.getMessage()));
    	    	    	
   		List<UserServiceVerification> userServiceVerifications = 
    			userServiceVerificationJpa.findByVerificationStatusAndUser(VerificationStatusServiceEY.PENDING, user);
   		
   		for(UserServiceVerification userServiceVerification : userServiceVerifications) {
   			userServiceVerification.setVerificationStatus(VerificationStatusServiceEY.ACTIVE);
    		
 			List<UserDocument> userDocuments = userDocumentJpa.findByUserServiceVerification(userServiceVerification);
 			
 			userDocumentJpa.deleteAll(userDocuments);
   		}
   		
   		userServiceVerificationJpa.deleteAll(userServiceVerifications); 
   		
   		List<UserOtp> userAllOtp = userOtpJpa.findByUser(user);
   		
   		userOtpJpa.deleteAll(userAllOtp);
   		
    	userJpa.delete(user);
    	
    	emailService.sendEmailForRejectAccount(user.getEmail(), motivation);
    }
    
    
    /*
     * Approverà a tutti i servizi annessi all'utente come stato pubblicato
     */
    private void servicesApprovePublicationStatus(User user) {
		List<NccServiceEntity> nccServices = nccServiceJpa.findByUser(user);
		if (nccServices.isEmpty() != false) {
			for(NccServiceEntity nccService : nccServices) {
				nccApprovePublicationStatus(nccService);
			}
		}

		List<RestaurantServiceEntity> restaurantServices = restaurantServiceJpa.findByUser(user);
		if (nccServices.isEmpty() != false) {
			for(RestaurantServiceEntity restaurantService : restaurantServices) {
				restaurantApprovePublicationStatus(restaurantService);
			}
		}

		List<ClubServiceEntity> clubServices = clubServiceJpa.findByUser(user);
		if (nccServices.isEmpty() != false) {
			for(ClubServiceEntity clubService : clubServices) {
				clubApprovePublicationStatus(clubService);
			}
		}

		List<LuggageServiceEntity> luggageServices = luggageServiceJpa.findByUser(user);
		if (nccServices.isEmpty() != false) {
			for(LuggageServiceEntity luggageService : luggageServices) {
				luggageApprovePublicationStatus(luggageService);
			}
		}

		List<BnbServiceEntity> bnbServices = bnbServiceJpa.findByUser(user);
		if (nccServices.isEmpty() != false) {
			for(BnbServiceEntity bnbService : bnbServices) {
				bnbApprovePublicationStatus(bnbService);
			}
		}
    }
    
    private void nccApprovePublicationStatus(NccServiceEntity entity) {
    	entity.setPublicationStatus(true);
    	nccServiceJpa.save(entity);
    }
    
    private void restaurantApprovePublicationStatus(RestaurantServiceEntity entity) {
    	entity.setPublicationStatus(true);
    	restaurantServiceJpa.save(entity);
    }
    
    private void clubApprovePublicationStatus(ClubServiceEntity entity) {
    	entity.setPublicationStatus(true);
    	clubServiceJpa.save(entity);    	
    }
    
    private void luggageApprovePublicationStatus(LuggageServiceEntity entity) {
    	entity.setPublicationStatus(true);
    	luggageServiceJpa.save(entity);
    }
    
    private void bnbApprovePublicationStatus(BnbServiceEntity entity) {
    	entity.setPublicationStatus(true);
    	bnbServiceJpa.save(entity);
    }
    
    
}
