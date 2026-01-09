package us.hogu.service.intefaces;

import java.util.List;

import us.hogu.controller.dto.response.PendingUserResponseDto;
import us.hogu.controller.dto.response.UserDocumentResponseDto;

public interface AdminService {

	List<PendingUserResponseDto> getProviderAccountsPending();

	void approveProviderAccount(Long idUser);

    void rejectProviderAccount(Long idUser, String motivation);

	UserDocumentResponseDto getFileUserDocument(Long idDocument);

}
