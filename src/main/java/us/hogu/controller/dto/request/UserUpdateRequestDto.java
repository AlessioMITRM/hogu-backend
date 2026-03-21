package us.hogu.controller.dto.request;

import java.util.List;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class UserUpdateRequestDto {
    @NotBlank
    private String name;

    private String surname;

    private String fiscalCode;

    private String iban;

    private List<ServiceLocaleRequestDto> locales;
}