package us.hogu.model.internal;

import java.util.List;

import lombok.Data;

@Data
public class Menu {
    private List<String> primi;
    
    private String secondo;
    
    private String dolce;
    
    private List<String> bevande;
    
    private List<String> vini;
}
