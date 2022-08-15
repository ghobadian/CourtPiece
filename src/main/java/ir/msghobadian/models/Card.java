package ir.msghobadian.models;

import ir.msghobadian.enums.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@AllArgsConstructor
@Builder
@Data
public class Card implements Serializable {
    private Type type;
    private int number;
}

