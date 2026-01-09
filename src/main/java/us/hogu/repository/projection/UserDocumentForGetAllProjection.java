package us.hogu.repository.projection;

public interface UserDocumentForGetAllProjection {
	Long getId();
	
	String getFilename();
	
	boolean isApproved();
}
