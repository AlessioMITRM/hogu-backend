package us.hogu.model.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderServicesCheck {
    private boolean hasBnb;
    private boolean hasClub;
    private boolean hasRestaurant;
    private boolean hasLuggage;
    private boolean hasNcc;
}