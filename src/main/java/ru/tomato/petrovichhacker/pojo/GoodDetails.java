package ru.tomato.petrovichhacker.pojo;

import lombok.Data;

import java.util.List;

@Data
public class GoodDetails {
    private String deliveryTime;
    private String availableForDeliveryAmount;

    private List<WarehouseDeliveryDetails> warehouseDeliveryDetailsList;
}
